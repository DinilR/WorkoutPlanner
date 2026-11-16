package com.workoutplanner.ui.plans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.data.db.PlanRow
import com.workoutplanner.ui.components.GradientTopBar
import com.workoutplanner.ui.theme.PunkButtonShape
import com.workoutplanner.ui.formatRest
import com.workoutplanner.ui.formatTarget
import com.workoutplanner.ui.formatWeight
import com.workoutplanner.ui.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    viewModel: PlanDetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenPlan: (Long) -> Unit,
    onWorkoutStarted: (sessionId: Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val conflict by viewModel.conflict.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    val plan = state?.plan

    Scaffold(
        topBar = {
            GradientTopBar(
                title = { Text(plan?.name.orEmpty().uppercase()) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (plan != null) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (!plan.isDefault) {
                                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; onEdit() })
                                }
                                DropdownMenuItem(
                                    text = { Text("Duplicate") },
                                    onClick = { menuOpen = false; viewModel.duplicate(onOpenPlan) },
                                )
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state?.rows?.isNotEmpty() == true) {
                Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                    Button(
                        onClick = { viewModel.start(onWorkoutStarted) },
                        shape = PunkButtonShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .height(56.dp),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("START WORKOUT", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
    ) { padding ->
        val current = state
        if (current?.plan == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                if (current == null) CircularProgressIndicator() else Text("This plan no longer exists.")
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            item {
                current.plan.description?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(current.plan.level.label) })
                    AssistChip(onClick = {}, label = { Text("${current.totalSets} sets") })
                    AssistChip(onClick = {}, label = { Text("~${current.estimatedMinutes} min") })
                }
                if (current.plan.isDefault) {
                    Text(
                        "Built-in plan. Use Duplicate to make your own editable copy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                PlanTableHeader()
                HorizontalDivider()
            }
            items(current.rows, key = { it.id }) { row ->
                PlanTableRow(row)
                HorizontalDivider()
            }
            item {
                Text(
                    "Tap an exercise to see how to do it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    conflict?.let { active ->
        AlertDialog(
            onDismissRequest = viewModel::dismissConflict,
            title = { Text("Unfinished workout") },
            text = {
                Text("You haven't finished ${active.planName} yet (${active.done} of ${active.total} sets). Resume it, or discard it and start this plan?")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.resumeExisting(onWorkoutStarted) }) { Text("Resume") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.discardAndStart(onWorkoutStarted) }) { Text("Discard & start new") }
            },
        )
    }
}

private val SetsWidth = 44.dp
private val RepsWidth = 52.dp
private val KgWidth = 60.dp
private val RestWidth = 52.dp

@Composable
private fun PlanTableHeader() {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        val style = MaterialTheme.typography.labelMedium
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        Text("Exercise", style = style, color = color, modifier = Modifier.weight(1f))
        Text("Sets", style = style, color = color, modifier = Modifier.width(SetsWidth), textAlign = TextAlign.Center)
        Text("Reps", style = style, color = color, modifier = Modifier.width(RepsWidth), textAlign = TextAlign.Center)
        Text("kg", style = style, color = color, modifier = Modifier.width(KgWidth), textAlign = TextAlign.Center)
        Text("Rest", style = style, color = color, modifier = Modifier.width(RestWidth), textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlanTableRow(row: PlanRow) {
    var expanded by rememberSaveable(row.id) { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(row.exerciseName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f, fill = false))
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Hide instructions" else "Show instructions",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("${row.sets}", modifier = Modifier.width(SetsWidth), textAlign = TextAlign.Center)
            Text(formatTarget(row.target, row.targetType), modifier = Modifier.width(RepsWidth), textAlign = TextAlign.Center)
            Text(formatWeight(row.weightKg)?.removeSuffix(" kg") ?: "—", modifier = Modifier.width(KgWidth), textAlign = TextAlign.Center)
            Text(formatRest(row.restSeconds), modifier = Modifier.width(RestWidth), textAlign = TextAlign.Center)
        }
        AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(top = 8.dp, end = 8.dp)) {
                row.muscleGroup?.let {
                    Text("Targets: $it", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                InstructionSteps(row.instructions)
            }
        }
    }
}

/** Numbered how-to steps, or a hint when an exercise has none. */
@Composable
fun InstructionSteps(instructions: String?) {
    val steps = instructions?.lines()?.filter { it.isNotBlank() }.orEmpty()
    if (steps.isEmpty()) {
        Text(
            "No instructions added for this exercise.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    steps.forEachIndexed { index, step ->
        Row(Modifier.padding(top = 4.dp)) {
            Text("${index + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
            Text(step, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
