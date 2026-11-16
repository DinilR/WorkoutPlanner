package com.workoutplanner.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.sound.SoundPlayer
import com.workoutplanner.sound.WorkoutSound

/** "All sounds" switch, plus a switch and a ▶ preview for each sound. Changes save immediately. */
@Composable
fun SoundSettingsDialog(player: SoundPlayer, onDismiss: () -> Unit) {
    val settings by player.settings.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SOUNDS") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("All sounds", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Plays on media volume, over your music",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = settings.master, onCheckedChange = player::setMaster)
                }
                HorizontalDivider(Modifier.padding(vertical = 12.dp))
                WorkoutSound.entries.forEach { sound ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sound.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                sound.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { player.preview(sound) }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Play ${sound.label} sound")
                        }
                        Spacer(Modifier.width(4.dp))
                        Switch(
                            checked = sound in settings.enabled,
                            onCheckedChange = { player.setEnabled(sound, it) },
                            enabled = settings.master,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
