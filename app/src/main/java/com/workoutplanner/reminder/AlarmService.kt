package com.workoutplanner.reminder

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.workoutplanner.WorkoutApp

/**
 * Rings the daily alarm. A foreground service keeps the process alive while the tone loops,
 * which a broadcast receiver can't. Stops on Snooze, Dismiss, opening the app, or after
 * [ReminderNotifications.ALARM_TIMEOUT_MS].
 */
class AlarmService : Service() {
    private val ringer by lazy { AlarmRinger(this) }
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notifications = (application as WorkoutApp).container.reminderScheduler.notifications
        val notification = notifications.buildRingingAlarm()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ReminderNotifications.ID_ALARM, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(ReminderNotifications.ID_ALARM, notification)
        }
        ringer.start()
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed({ stopSelf() }, ReminderNotifications.ALARM_TIMEOUT_MS)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        ringer.stop()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
}
