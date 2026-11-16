package com.workoutplanner.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.workoutplanner.MainActivity
import com.workoutplanner.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The two reminder notifications. The daily alarm's channel is silent because [AlarmRinger]
 * plays the tone itself on the alarm stream: a notification sound can be muted by silent mode,
 * phone-maker rules or Android 15's notification cooldown. The smart reminder is a normal
 * notification.
 */
class ReminderNotifications(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    /** Channels are created once; Android keeps the user's changes to them after that. */
    fun createChannels() {
        val alarm = NotificationChannel(CHANNEL_ALARM, "Workout alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Shows while the alarm you set in Reminders is ringing"
            setSound(null, null)
            enableVibration(false)
        }
        val smart = NotificationChannel(CHANNEL_SMART, "Daily workout nudge", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "One hour before the time you last trained"
        }
        val system = context.getSystemService(NotificationManager::class.java)
        // The first version's alarm channel had its own sound, which would now play twice.
        system.deleteNotificationChannel(OLD_CHANNEL_ALARM)
        system.createNotificationChannels(listOf(alarm, smart))
    }

    fun canPost(): Boolean =
        manager.areNotificationsEnabled() && (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            )

    /** The notification shown while the alarm rings, with Snooze and Dismiss. */
    fun buildRingingAlarm(): Notification =
        NotificationCompat.Builder(context, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setColor(ContextCompat.getColor(context, R.color.ic_launcher_background))
            .setContentTitle("TIME TO TRAIN")
            .setContentText("Your workout alarm. Let's go!")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(openApp(stopAlarm = true))
            .setDeleteIntent(action(ReminderReceiver.ACTION_DISMISS))
            .addAction(0, "Snooze $SNOOZE_MINUTES min", action(ReminderReceiver.ACTION_SNOOZE))
            .addAction(0, "Dismiss", action(ReminderReceiver.ACTION_DISMISS))
            .setOngoing(true)
            .setTimeoutAfter(ALARM_TIMEOUT_MS)
            .build()

    /** Posts the ringing notification without the service, when Android won't let it start. */
    fun showRingingAlarm() {
        if (canPost()) notify(ID_ALARM, buildRingingAlarm())
    }

    /** "You trained at 5:00 PM last time", plus the streak when there is one to keep. */
    fun showSmart(lastStartedAt: Long, streak: Int) {
        if (!canPost()) return
        val time = Instant.ofEpochMilli(lastStartedAt).atZone(ZoneId.systemDefault()).format(timeFormat)
        val text = if (streak > 0) {
            "$streak-day streak. Train around $time again to keep it going."
        } else {
            "You trained at $time last time. Time to get today's workout in."
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_SMART)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setColor(ContextCompat.getColor(context, R.color.ic_launcher_background))
            .setContentTitle("Workout in an hour?")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        notify(ID_SMART, notification)
    }

    fun cancelAlarm() = manager.cancel(ID_ALARM)

    fun cancelAll() {
        manager.cancel(ID_ALARM)
        manager.cancel(ID_SMART)
    }

    @Suppress("MissingPermission") // checked in canPost()
    private fun notify(id: Int, notification: Notification) = manager.notify(id, notification)

    /** Opens the app; with [stopAlarm], opening it also silences the alarm. */
    private fun openApp(stopAlarm: Boolean = false): PendingIntent = PendingIntent.getActivity(
        context,
        if (stopAlarm) 1 else 0,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_STOP_ALARM, stopAlarm),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun action(name: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        name.hashCode(),
        Intent(context, ReminderReceiver::class.java).setAction(name),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val SNOOZE_MINUTES = 10
        const val EXTRA_STOP_ALARM = "com.workoutplanner.STOP_ALARM"
        /** Ringing stops on its own if the phone is left alone this long. */
        const val ALARM_TIMEOUT_MS = 10 * 60_000L
        const val ID_ALARM = 1001
        private const val OLD_CHANNEL_ALARM = "workout_alarm"
        private const val CHANNEL_ALARM = "workout_alarm_ringing"
        private const val CHANNEL_SMART = "workout_smart"
        private const val ID_SMART = 1002
        private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
    }
}
