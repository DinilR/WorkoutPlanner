package com.workoutplanner

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.workoutplanner.reminder.ReminderNotifications
import com.workoutplanner.ui.AppNavHost
import com.workoutplanner.ui.theme.WorkoutPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light status-bar icons, since every screen has a Red Orange top bar.
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        val container = (application as WorkoutApp).container
        stopAlarmIfAsked(intent)
        setContent {
            WorkoutPlannerTheme {
                AppNavHost(container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        stopAlarmIfAsked(intent)
    }

    /** Tapping the ringing alarm's notification opens the app and silences it. */
    private fun stopAlarmIfAsked(intent: Intent?) {
        if (intent?.getBooleanExtra(ReminderNotifications.EXTRA_STOP_ALARM, false) == true) {
            (application as WorkoutApp).container.reminderScheduler.dismissAlarm()
        }
    }
}
