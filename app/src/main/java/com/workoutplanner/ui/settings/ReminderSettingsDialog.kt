package com.workoutplanner.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.reminder.ReminderScheduler
import com.workoutplanner.ui.formatDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** The daily alarm (switch + time) and the smart reminder (switch + next time). Changes save immediately. */
@Composable
fun ReminderSettingsDialog(scheduler: ReminderScheduler, onDismiss: () -> Unit) {
    val settings by scheduler.settings.collectAsStateWithLifecycle()
    val nextSmartAt by scheduler.nextSmartAt.collectAsStateWithLifecycle()
    val nextDailyAt by scheduler.nextDailyAt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pickingTime by remember { mutableStateOf(false) }

    // Both can change in system settings while the dialog is open, so re-check on return.
    var canNotify by remember { mutableStateOf(scheduler.notifications.canPost()) }
    var canExact by remember { mutableStateOf(scheduler.canScheduleExact()) }
    var batteryOk by remember { mutableStateOf(scheduler.isBatteryUnrestricted()) }
    LifecycleResumeEffect(Unit) {
        canNotify = scheduler.notifications.canPost()
        canExact = scheduler.canScheduleExact()
        batteryOk = scheduler.isBatteryUnrestricted()
        onPauseOrDispose { }
    }
    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
        }
    }
    fun test(smart: Boolean) {
        scheduler.scheduleTest(smart)
        Toast.makeText(context, "Test reminder in 1 minute. You can close the app.", Toast.LENGTH_LONG).show()
    }
    val askNotify = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = granted && scheduler.notifications.canPost()
        if (canNotify && scheduler.settings.value.dailyOn && !scheduler.canScheduleExact()) openExactAlarmSettings()
    }
    fun ensureNotifications() {
        if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            askNotify.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("REMINDERS") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SwitchRow(
                    title = "Daily alarm",
                    description = "Rings like an alarm every day until you snooze or dismiss it.",
                    checked = settings.dailyOn,
                    onCheckedChange = {
                        scheduler.setDailyEnabled(it)
                        // Notification permission first; exact alarms are asked for once that's settled.
                        if (it && !canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ensureNotifications()
                        } else if (it && !canExact) {
                            openExactAlarmSettings()
                        }
                    },
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Time", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { pickingTime = true }) {
                        Text(settings.dailyTime.format(timeFormat), style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (settings.dailyOn) {
                    nextDailyAt?.let {
                        Text(
                            "Next: ${formatDateTime(it)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                TextButton(onClick = { test(smart = false) }) { Text("Test alarm in 1 min") }
                if (settings.dailyOn && !canExact) {
                    Warning(
                        text = "Exact alarms are off, so the alarm may ring up to 10 minutes late.",
                        action = "Allow exact alarms",
                        onAction = ::openExactAlarmSettings,
                    )
                }

                HorizontalDivider(Modifier.padding(vertical = 12.dp))

                SwitchRow(
                    title = "Smart reminder",
                    description = "A nudge every day one hour before the time you started your last workout, until you train again.",
                    checked = settings.smartOn,
                    onCheckedChange = {
                        if (it) ensureNotifications()
                        scheduler.setSmartEnabled(it)
                    },
                )
                if (settings.smartOn) {
                    Text(
                        nextSmartAt?.let { "Next: ${formatDateTime(it)}" } ?: "Finish a workout to set the time.",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(onClick = { test(smart = true) }) { Text("Test reminder in 1 min") }

                if ((settings.dailyOn || settings.smartOn) && !canNotify) {
                    Spacer(Modifier.height(12.dp))
                    Warning(
                        text = "Notifications are blocked, so reminders can't appear.",
                        action = "Allow notifications",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            )
                        },
                    )
                }

                if ((settings.dailyOn || settings.smartOn) && !batteryOk) {
                    Spacer(Modifier.height(12.dp))
                    Warning(
                        text = "Battery optimisation can stop reminders when the app is closed. Set Workout Planner to \"Unrestricted\" or \"Don't optimise\".",
                        action = "Battery settings",
                        onAction = { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )

    if (pickingTime) {
        TimePickerDialog(
            hour = settings.dailyHour,
            minute = settings.dailyMinute,
            onDismiss = { pickingTime = false },
            onConfirm = { hour, minute ->
                scheduler.setDailyTime(hour, minute)
                pickingTime = false
            },
        )
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun Warning(text: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(text, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ALARM TIME") },
        text = { TimePicker(state = state) },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("Set") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
