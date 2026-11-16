package com.workoutplanner.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workoutplanner.WorkoutApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives reminder alarms, the alarm's Snooze/Dismiss buttons, and the system events that clear alarms. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = (context.applicationContext as WorkoutApp).container.reminderScheduler
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_DAILY_ALARM -> scheduler.onDailyAlarm(snoozed = false)
                    ACTION_SNOOZED_ALARM -> scheduler.onDailyAlarm(snoozed = true)
                    ACTION_SMART -> scheduler.onSmartAlarm()
                    ACTION_TEST_ALARM -> scheduler.onTest(smart = false)
                    ACTION_TEST_SMART -> scheduler.onTest(smart = true)
                    ACTION_SNOOZE -> scheduler.snooze()
                    ACTION_DISMISS -> scheduler.dismissAlarm()
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
                    -> scheduler.rescheduleAll()
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DAILY_ALARM = "com.workoutplanner.reminder.DAILY_ALARM"
        const val ACTION_SNOOZED_ALARM = "com.workoutplanner.reminder.SNOOZED_ALARM"
        const val ACTION_SMART = "com.workoutplanner.reminder.SMART"
        const val ACTION_TEST_ALARM = "com.workoutplanner.reminder.TEST_ALARM"
        const val ACTION_TEST_SMART = "com.workoutplanner.reminder.TEST_SMART"
        const val ACTION_SNOOZE = "com.workoutplanner.reminder.SNOOZE"
        const val ACTION_DISMISS = "com.workoutplanner.reminder.DISMISS"
    }
}
