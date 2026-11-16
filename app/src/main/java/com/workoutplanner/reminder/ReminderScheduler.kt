package com.workoutplanner.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.workoutplanner.MainActivity
import com.workoutplanner.data.repo.SessionRepository
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

data class ReminderSettings(
    /** The alarm-style reminder at a fixed time each day. */
    val dailyOn: Boolean = false,
    val dailyHour: Int = 18,
    val dailyMinute: Int = 0,
    /** The nudge one hour before the time of day the last workout started. */
    val smartOn: Boolean = false,
) {
    val dailyTime: LocalTime get() = LocalTime.of(dailyHour, dailyMinute)
}

/**
 * Schedules both reminders with AlarmManager and decides, when one fires, whether to show it.
 * The daily alarm always rings; the smart reminder is skipped on a day a workout is already done.
 *
 * Alarms are exact when the user allows it (Android 12+ asks, and 14+ starts with it off),
 * otherwise they land within [INEXACT_WINDOW_MS] of the set time. Alarms are lost on reboot,
 * so [ReminderReceiver] calls [rescheduleAll] after boot and after the clock or time zone changes.
 */
class ReminderScheduler(
    private val context: Context,
    private val sessions: SessionRepository,
    private val scope: CoroutineScope,
) {
    val notifications = ReminderNotifications(context)

    /** Rings from the receiver when Android refuses to start [AlarmService]. */
    private val fallbackRinger = AlarmRinger(context)

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(AlarmManager::class.java)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<ReminderSettings> = _settings.asStateFlow()

    /** When the smart reminder will next fire, or null if it isn't scheduled. */
    private val _nextSmartAt = MutableStateFlow<Long?>(null)
    val nextSmartAt: StateFlow<Long?> = _nextSmartAt.asStateFlow()

    private val _nextDailyAt = MutableStateFlow<Long?>(null)
    val nextDailyAt: StateFlow<Long?> = _nextDailyAt.asStateFlow()

    init {
        notifications.createChannels()
        rescheduleAll()
        // A newly finished (or deleted) workout moves the smart reminder, and a finished one
        // makes any reminder still on screen pointless.
        scope.launch {
            sessions.observeLastFinishedStart().distinctUntilChanged().drop(1).collect {
                if (sessions.trainedToday()) {
                    dismissAlarm()
                    notifications.cancelAll()
                }
                scheduleSmart()
            }
        }
    }

    fun canScheduleExact(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()

    /** False when battery optimisation may stop alarms once the app is swiped away (common on Samsung, Xiaomi, Oppo). */
    fun isBatteryUnrestricted(): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    fun setDailyEnabled(on: Boolean) {
        _settings.update { it.copy(dailyOn = on) }
        prefs.edit().putBoolean(KEY_DAILY_ON, on).apply()
        scheduleDaily()
    }

    fun setDailyTime(hour: Int, minute: Int) {
        _settings.update { it.copy(dailyHour = hour, dailyMinute = minute) }
        prefs.edit().putInt(KEY_DAILY_HOUR, hour).putInt(KEY_DAILY_MINUTE, minute).apply()
        scheduleDaily()
    }

    fun setSmartEnabled(on: Boolean) {
        _settings.update { it.copy(smartOn = on) }
        prefs.edit().putBoolean(KEY_SMART_ON, on).apply()
        scope.launch { scheduleSmart() }
    }

    fun rescheduleAll() {
        scheduleDaily()
        scope.launch { scheduleSmart() }
    }

    /** Called by [ReminderReceiver] when the daily alarm (or its snooze) goes off. */
    suspend fun onDailyAlarm(snoozed: Boolean) {
        if (!snoozed) scheduleDaily()
        if (_settings.value.dailyOn) ring()
    }

    /**
     * Starts [AlarmService], which loops the alarm tone until Snooze or Dismiss. Android only
     * lets a background app start it from an exact alarm; otherwise (exact alarms off) the tone
     * plays from the receiver for as long as Android keeps it alive.
     */
    private suspend fun ring() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java))
        } catch (_: IllegalStateException) {
            // ForegroundServiceStartNotAllowedException (Android 12+) is an IllegalStateException.
            notifications.showRingingAlarm()
            fallbackRinger.start()
            delay(FALLBACK_RING_MS)
            fallbackRinger.stop()
        }
    }

    /** Silences the alarm and removes its notification. */
    fun dismissAlarm() {
        context.stopService(Intent(context, AlarmService::class.java))
        fallbackRinger.stop()
        notifications.cancelAlarm()
    }

    suspend fun onSmartAlarm() {
        val lastStart = sessions.lastFinishedStart()
        scheduleSmart()
        if (_settings.value.smartOn && lastStart != null && !sessions.trainedToday()) {
            notifications.showSmart(lastStart, sessions.currentStreak(includeToday = false))
        }
    }

    /**
     * Fires a reminder through the real path (AlarmManager → receiver → notification) in one
     * minute, ignoring the on/off switches, so the user can check it works on their phone.
     */
    fun scheduleTest(smart: Boolean) {
        val action = if (smart) ReminderReceiver.ACTION_TEST_SMART else ReminderReceiver.ACTION_TEST_ALARM
        setAlarm(System.currentTimeMillis() + 60_000L, pendingIntent(action), alarmClock = !smart)
    }

    suspend fun onTest(smart: Boolean) {
        if (smart) {
            notifications.showSmart(sessions.lastFinishedStart() ?: System.currentTimeMillis(), sessions.currentStreak(includeToday = false))
        } else {
            ring()
        }
    }

    fun snooze() {
        dismissAlarm()
        val at = System.currentTimeMillis() + ReminderNotifications.SNOOZE_MINUTES * 60_000L
        setAlarm(at, pendingIntent(ReminderReceiver.ACTION_SNOOZED_ALARM), alarmClock = true)
    }

    private fun scheduleDaily() {
        val intent = pendingIntent(ReminderReceiver.ACTION_DAILY_ALARM)
        val settings = _settings.value
        if (!settings.dailyOn) {
            alarms.cancel(intent)
            alarms.cancel(pendingIntent(ReminderReceiver.ACTION_SNOOZED_ALARM))
            _nextDailyAt.value = null
            return
        }
        val at = nextOccurrence(settings.dailyTime)
        setAlarm(at, intent, alarmClock = true)
        _nextDailyAt.value = at
    }

    /**
     * Fires daily at one hour before the time of day the last finished workout started,
     * e.g. last workout at 5:30 PM → 4:30 PM every day until the next workout moves it.
     */
    private suspend fun scheduleSmart() {
        val intent = pendingIntent(ReminderReceiver.ACTION_SMART)
        val lastStart = sessions.lastFinishedStart()
        if (!_settings.value.smartOn || lastStart == null) {
            alarms.cancel(intent)
            _nextSmartAt.value = null
            return
        }
        val time = Instant.ofEpochMilli(lastStart).atZone(ZoneId.systemDefault())
            .minusHours(1)
            .toLocalTime()
            .truncatedTo(ChronoUnit.MINUTES)
        val at = nextOccurrence(time)
        setAlarm(at, intent, alarmClock = false)
        _nextSmartAt.value = at
    }

    /** The next time the clock reads [time], today if it's still ahead, otherwise tomorrow. */
    private fun nextOccurrence(time: LocalTime): Long {
        val now = ZonedDateTime.now()
        var at = now.with(time)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return at.toInstant().toEpochMilli()
    }

    private fun setAlarm(at: Long, intent: PendingIntent, alarmClock: Boolean) {
        try {
            when {
                !canScheduleExact() -> setInexact(at, intent)
                // Shows the alarm-clock icon in the status bar and is never deferred.
                alarmClock -> alarms.setAlarmClock(AlarmManager.AlarmClockInfo(at, showApp()), intent)
                else -> alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission was revoked between the check and the call.
            setInexact(at, intent)
        }
    }

    /**
     * Without exact-alarm permission. A plain inexact alarm set a day ahead can be delivered
     * hours late, so a bounded window is used instead.
     */
    private fun setInexact(at: Long, intent: PendingIntent) =
        alarms.setWindow(AlarmManager.RTC_WAKEUP, at, INEXACT_WINDOW_MS, intent)

    private fun pendingIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        action.hashCode(),
        Intent(context, ReminderReceiver::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun showApp(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun loadSettings() = ReminderSettings(
        dailyOn = prefs.getBoolean(KEY_DAILY_ON, false),
        dailyHour = prefs.getInt(KEY_DAILY_HOUR, 18),
        dailyMinute = prefs.getInt(KEY_DAILY_MINUTE, 0),
        smartOn = prefs.getBoolean(KEY_SMART_ON, false),
    )

    private companion object {
        /** The smallest window Android 12+ allows for an inexact alarm. */
        const val INEXACT_WINDOW_MS = 10 * 60_000L
        /** Kept under the ~60 s Android gives a background broadcast receiver. */
        const val FALLBACK_RING_MS = 45_000L
        const val KEY_DAILY_ON = "reminder_daily_on"
        const val KEY_DAILY_HOUR = "reminder_daily_hour"
        const val KEY_DAILY_MINUTE = "reminder_daily_minute"
        const val KEY_SMART_ON = "reminder_smart_on"
    }
}
