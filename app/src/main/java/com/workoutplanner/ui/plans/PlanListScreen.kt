package com.workoutplanner.ui.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.R
import com.workoutplanner.data.db.ActiveSessionSummary
import com.workoutplanner.data.db.Level
import com.workoutplanner.data.db.PlanSummary
import com.workoutplanner.reminder.ReminderScheduler
import com.workoutplanner.sound.SoundPlayer
import com.workoutplanner.ui.components.diagonalStripes
import com.workoutplanner.ui.settings.ReminderSettingsDialog
import com.workoutplanner.ui.settings.SoundSettingsDialog
import com.workoutplanner.ui.label
import com.workoutplanner.ui.theme.LocalWorkoutColors
import com.workoutplanner.ui.theme.PunkButtonShape
import com.workoutplanner.ui.theme.brandGradient

@Composable
fun PlanListScreen(
    viewModel: PlanListViewModel,
    onOpenPlan: (Long) -> Unit,
    onNewPlan: () -> Unit,
    onEditPlan: (Long) -> Unit,
    onResume: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    soundPlayer: SoundPlayer,
    reminderScheduler: ReminderScheduler,
) {
    val plans by viewModel.plansState.collectAsStateWithLifecycle()
    val active by viewModel.activeSession.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<PlanSummary?>(null) }
    var soundSettingsOpen by remember { mutableStateOf(false) }
    var remindersOpen by remember { mutableStateOf(false) }

    Scaffold(
        // The hero runs up behind the status bar, so only the bottom inset is applied.
        contentWindowInsets = WindowInsets.navigationBars,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewPlan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("NEW PLAN", style = MaterialTheme.typography.labelLarge) },
            )
        },
    ) { padding ->
        val list = plans
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                Hero(
                    planCount = list?.size,
                    onOpenSettings = { soundSettingsOpen = true },
                    onOpenReminders = { remindersOpen = true },
                    onOpenHistory = onOpenHistory,
                )
            }
            if (list == null) {
                item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                return@LazyColumn
            }
            active?.let { session ->
                item(key = "resume") {
                    ResumeBanner(session, onClick = { onResume(session.sessionId) }, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            item(key = "header") {
                Text(
                    "YOUR PLANS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                )
            }
            items(list, key = { it.plan.id }) { summary ->
                PlanCard(
                    summary = summary,
                    onClick = { onOpenPlan(summary.plan.id) },
                    onEdit = { onEditPlan(summary.plan.id) },
                    onDuplicate = { viewModel.duplicate(summary.plan.id) },
                    onDelete = { pendingDelete = summary },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (soundSettingsOpen) {
        SoundSettingsDialog(player = soundPlayer, onDismiss = { soundSettingsOpen = false })
    }

    if (remindersOpen) {
        ReminderSettingsDialog(scheduler = reminderScheduler, onDismiss = { remindersOpen = false })
    }

    pendingDelete?.let { summary ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete plan?") },
            text = { Text("\"${summary.plan.name}\" will be removed. Past workouts stay in your history.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(summary.plan.id)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

/** Poster-style header: gradient, racing stripes and a big LET'S TRAIN. */
@Composable
private fun Hero(planCount: Int?, onOpenSettings: () -> Unit, onOpenReminders: () -> Unit, onOpenHistory: () -> Unit) {
    val yellow = LocalWorkoutColors.current.done
    Box(
        Modifier
            .fillMaxWidth()
            .clip(CutCornerShape(bottomEnd = 48.dp))
            .background(brandGradient())
            .diagonalStripes(Color.White.copy(alpha = 0.09f), spacing = 30.dp, width = 12.dp)
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp))
                        .background(yellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_logo),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(34.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "WORKOUT PLANNER",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onOpenReminders) {
                    Icon(Icons.Default.Notifications, contentDescription = "Reminders", tint = Color.White)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Sound settings", tint = Color.White)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("LET'S", style = MaterialTheme.typography.displaySmall, color = Color.White)
            Text("TRAIN.", style = MaterialTheme.typography.displayLarge, color = yellow)
            if (planCount != null) {
                Text(
                    "$planCount plans ready · works offline",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onOpenHistory,
                shape = PunkButtonShape,
                colors = ButtonDefaults.buttonColors(containerColor = yellow, contentColor = LocalWorkoutColors.current.onDone),
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("HISTORY", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ResumeBanner(session: ActiveSessionSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalWorkoutColors.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = colors.done, contentColor = colors.onDone),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("RESUME ${session.planName.uppercase()}", style = MaterialTheme.typography.titleMedium)
                Text("${session.done} of ${session.total} sets done", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { if (session.total == 0) 0f else session.done.toFloat() / session.total },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = colors.onDone.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Edge colour of a plan card, by level. */
@Composable
private fun levelAccent(level: Level): Color = when (level) {
    Level.BEGINNER -> LocalWorkoutColors.current.done
    Level.INTERMEDIATE -> MaterialTheme.colorScheme.primary
    Level.ADVANCED -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun PlanCard(
    summary: PlanSummary,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val plan = summary.plan
    var menuOpen by remember { mutableStateOf(false) }

    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(10.dp).fillMaxHeight().background(levelAccent(plan.level)))
            Row(Modifier.weight(1f).padding(start = 14.dp, top = 12.dp, bottom = 16.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            plan.name.uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (plan.isDefault) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Default plan",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    plan.description?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${plan.level.label.uppercase()} · ${summary.exerciseCount} EXERCISES · ${summary.totalSets} SETS · ~${summary.estimatedMinutes} MIN",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options for ${plan.name}")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (!plan.isDefault) {
                            DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                        }
                        DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menuOpen = false; onDuplicate() })
                        if (!plan.isDefault) {
                            DropdownMenuItem(text = { Text("Delete") }, onClick = { menuOpen = false; onDelete() })
                        }
                    }
                }
            }
        }
    }
}
