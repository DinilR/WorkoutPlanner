package com.workoutplanner.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.data.db.SetLog
import com.workoutplanner.ui.components.GradientTopBar
import com.workoutplanner.ui.emoji
import com.workoutplanner.ui.formatDateTime
import com.workoutplanner.ui.formatMinutes
import com.workoutplanner.ui.formatTarget
import com.workoutplanner.ui.formatVolume
import com.workoutplanner.ui.formatWeight
import com.workoutplanner.ui.theme.LocalWorkoutColors
import com.workoutplanner.ui.workout.ExerciseGroup

@Composable
fun SessionDetailScreen(
    viewModel: SessionDetailViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = { Text(state?.session?.planName.orEmpty().uppercase()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Delete from history") },
                                onClick = { menuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val session = current.session
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") {
                Column {
                    Text(
                        formatDateTime(session.startedAt).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryStat("TIME", formatMinutes((session.finishedAt ?: session.startedAt) - session.startedAt), Modifier.weight(1f))
                        SummaryStat("SETS", "${current.done}/${current.total}", Modifier.weight(1f))
                        SummaryStat("VOLUME", formatVolume(current.volumeKg), Modifier.weight(1f))
                        session.rating?.let { SummaryStat("FELT", it.emoji, Modifier.weight(1f)) }
                    }
                }
            }
            items(current.groups, key = { it.order }) { group -> ExerciseResultCard(group) }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this workout?") },
            text = { Text("It will be removed from your history, streak and \"last time\" hints.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete(onBack) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val SetWidth = 56.dp
private val PlannedWidth = 72.dp
private val KgWidth = 64.dp

/** Set-by-set table: what was planned, what was actually done, and the weight. */
@Composable
private fun ExerciseResultCard(group: ExerciseGroup) {
    val colors = LocalWorkoutColors.current
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (group.isDone) colors.doneContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(group.name.uppercase(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text("${group.doneCount}/${group.sets.size}", style = MaterialTheme.typography.labelLarge)
            }
            if (group.volumeKg > 0) {
                Text(formatVolume(group.volumeKg), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                HeaderText("Set", Modifier.width(SetWidth), TextAlign.Start)
                HeaderText("Planned", Modifier.width(PlannedWidth))
                HeaderText("Done", Modifier.weight(1f))
                HeaderText("kg", Modifier.width(KgWidth))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            group.sets.forEach { set -> SetRow(set) }
        }
    }
}

@Composable
private fun HeaderText(text: String, modifier: Modifier, align: TextAlign = TextAlign.Center) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        modifier = modifier.padding(vertical = 4.dp),
    )
}

@Composable
private fun SetRow(set: SetLog) {
    val done = set.completedAt != null
    val doneValue = set.actualValue ?: set.targetValue
    val belowPlan = done && doneValue < set.targetValue
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("${set.setNumber}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(SetWidth))
        Text(
            formatTarget(set.targetValue, set.targetType),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(PlannedWidth),
        )
        Text(
            if (done) "✓ ${formatTarget(doneValue, set.targetType)}" else "skipped",
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                !done -> MaterialTheme.colorScheme.onSurfaceVariant
                belowPlan -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Text(
            formatWeight(set.weightKg)?.removeSuffix(" kg") ?: "—",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(KgWidth),
        )
    }
}
