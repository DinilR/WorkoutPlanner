package com.workoutplanner.ui.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.data.db.SetLog
import com.workoutplanner.data.db.TargetType
import com.workoutplanner.sound.SoundPlayer
import com.workoutplanner.ui.components.GradientTopBar
import com.workoutplanner.ui.settings.SoundSettingsDialog
import com.workoutplanner.ui.components.Stepper
import com.workoutplanner.ui.theme.PunkButtonShape
import com.workoutplanner.ui.formatDuration
import com.workoutplanner.ui.formatLastTime
import com.workoutplanner.ui.formatRest
import com.workoutplanner.ui.formatTarget
import com.workoutplanner.ui.formatVolume
import com.workoutplanner.ui.formatWeight
import com.workoutplanner.ui.plans.InstructionSteps
import com.workoutplanner.ui.theme.LocalWorkoutColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onDiscarded: () -> Unit,
    soundPlayer: SoundPlayer,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val rest by viewModel.rest.collectAsStateWithLifecycle()
    val lastTime by viewModel.lastTime.collectAsStateWithLifecycle()
    var soundSettingsOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SetLog?>(null) }
    // Cards the user opened by hand, on top of the current exercise.
    var opened by rememberSaveable { mutableStateOf(setOf<Int>()) }

    // The screen stays on for the whole workout.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(200)
        }
    }

    // Rest countdown: a tick on each whole second (the set-done sound covers the first one),
    // then vibration and the rest-over sound at zero.
    LaunchedEffect(rest) {
        val running = rest ?: return@LaunchedEffect
        var first = true
        while (true) {
            val left = running.endsAt - System.currentTimeMillis()
            if (left <= 0) break
            val secondsLeft = ((left + 999) / 1000).toInt()
            if (!first) viewModel.restTick(secondsLeft)
            first = false
            delay(left - (secondsLeft - 1) * 1000L)
        }
        context.vibrateRestOver()
        viewModel.restFinished(running)
    }

    // Bring the current exercise into view when it changes. Item 0 is the progress header,
    // which stays visible while the first exercise is current.
    val currentIndex = state?.currentIndex ?: -1
    LaunchedEffect(currentIndex) {
        if (currentIndex > 0) listState.animateScrollToItem(currentIndex + 1)
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = {
                    Column {
                        Text(state?.session?.planName.orEmpty().uppercase())
                        state?.let {
                            Text(
                                formatDuration(now - it.session.startedAt),
                                style = MaterialTheme.typography.titleMedium,
                                color = LocalWorkoutColors.current.done,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back (workout is saved)")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Sound settings") },
                                onClick = { menuOpen = false; soundSettingsOpen = true },
                            )
                            DropdownMenuItem(
                                text = { Text("Discard workout") },
                                onClick = { menuOpen = false; confirmDiscard = true },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            val current = state ?: return@Scaffold
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                    rest?.let { running ->
                        RestBar(
                            remainingMs = running.endsAt - now,
                            totalSeconds = running.totalSeconds,
                            onAdd = { viewModel.addRestTime(30) },
                            onSkip = viewModel::skipRest,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    val finish: () -> Unit = { if (current.allDone) onFinish() else confirmFinish = true }
                    if (current.allDone) {
                        Button(onClick = finish, shape = PunkButtonShape, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text("FINISH WORKOUT", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        FilledTonalButton(onClick = finish, shape = PunkButtonShape, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Text("FINISH WORKOUT", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val restRunning = rest
        val nextSetId = restRunning?.nextSetId
            ?: current.groups.getOrNull(current.currentIndex)?.sets?.firstOrNull { it.completedAt == null }?.id

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                WorkoutHeader(
                    done = current.done,
                    total = current.total,
                    volumeKg = current.volumeKg,
                    allDone = current.allDone,
                )
            }
            itemsIndexed(current.groups, key = { _, g -> g.order }) { index, group ->
                ExerciseCard(
                    group = group,
                    isCurrent = index == current.currentIndex,
                    expanded = index == current.currentIndex || group.order in opened,
                    onToggle = { opened = if (group.order in opened) opened - group.order else opened + group.order },
                    lastTime = lastTime[group.exerciseId],
                    nextSetId = nextSetId,
                    restRemaining = restRunning?.let { ((it.endsAt - now + 999) / 1000).toInt().coerceAtLeast(0) },
                    restFraction = restRunning?.let { ((it.endsAt - now).toFloat() / (it.totalSeconds * 1000f)).coerceIn(0f, 1f) } ?: 0f,
                    onTapSet = { log ->
                        if (log.completedAt == null) {
                            viewModel.completeSet(log)
                        } else {
                            viewModel.undoSet(log)
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                val result = snackbar.showSnackbar("Set ${log.setNumber} of ${log.exerciseName} unmarked", "Undo")
                                if (result == SnackbarResult.ActionPerformed) viewModel.completeSet(log.copy(completedAt = null))
                            }
                        }
                    },
                    onEditSet = { editing = it },
                )
            }
        }
    }

    if (soundSettingsOpen) {
        SoundSettingsDialog(player = soundPlayer, onDismiss = { soundSettingsOpen = false })
    }

    editing?.let { log ->
        SetEditDialog(
            log = log,
            onDismiss = { editing = null },
            onSave = { value, weight ->
                viewModel.completeSet(log, value, weight)
                editing = null
            },
        )
    }

    if (confirmFinish) {
        val left = state?.let { it.total - it.done } ?: 0
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish early?") },
            text = { Text("You still have $left ${if (left == 1) "set" else "sets"} left. Finish the workout anyway?") },
            confirmButton = { TextButton(onClick = { confirmFinish = false; onFinish() }) { Text("Finish") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep going") } },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard workout?") },
            text = { Text("All sets ticked in this workout will be deleted.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; viewModel.discard(onDiscarded) }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WorkoutHeader(done: Int, total: Int, volumeKg: Double, allDone: Boolean) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        ProgressRing(done = done, total = total)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Stat("Volume", formatVolume(volumeKg))
            Stat("Sets left", "${total - done}")
        }
        AnimatedVisibility(visible = allDone) {
            Text(
                "All sets done! Tap Finish workout 🎉",
                style = MaterialTheme.typography.titleMedium,
                color = LocalWorkoutColors.current.doneText,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseCard(
    group: ExerciseGroup,
    isCurrent: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    /** Sets done for this exercise in the previous workout, or null if it's the first time. */
    lastTime: List<SetLog>?,
    nextSetId: Long?,
    restRemaining: Int?,
    restFraction: Float,
    onTapSet: (SetLog) -> Unit,
    onEditSet: (SetLog) -> Unit,
) {
    val colors = LocalWorkoutColors.current
    var showHowTo by rememberSaveable(group.order) { mutableStateOf(false) }
    val first = group.sets.first()

    Card(
        onClick = onToggle,
        enabled = !isCurrent,
        colors = CardDefaults.cardColors(
            containerColor = when {
                group.isDone -> colors.doneContainer
                isCurrent -> MaterialTheme.colorScheme.surfaceContainerHigh
                else -> MaterialTheme.colorScheme.surfaceContainer
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        // The exercise you're on gets a bold Red Orange frame.
        border = if (isCurrent) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (group.isDone) {
                    // Finished cards are solid Yellow Punk, so the tick badge is inverted.
                    Surface(shape = CircleShape, color = colors.onDone, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Check, contentDescription = "Done", tint = colors.done, modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(group.name.uppercase(), style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (group.isDone && !expanded) {
                            buildString {
                                append("${group.sets.size} sets")
                                if (group.volumeKg > 0) append(" · ${formatVolume(group.volumeKg)}")
                            }
                        } else {
                            buildString {
                                append("${group.sets.size} × ${formatTarget(first.targetValue, first.targetType)}")
                                if (first.targetType == TargetType.REPS) append(" reps")
                                append(" · ${formatWeight(first.weightKg) ?: "bodyweight"}")
                                append(" · rest ${formatRest(group.restSeconds)}")
                            }
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (lastTime != null && !group.isDone) {
                        Text(
                            formatLastTime(lastTime),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                when {
                    isCurrent -> Text("NOW", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    !group.isDone -> Text("${group.doneCount}/${group.sets.size}", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (expanded) {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        // Room for the pulse and pop animations, which grow past the bubble.
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    group.sets.forEach { log ->
                        val waiting = restRemaining != null && log.id == nextSetId
                        SetBubble(
                            log = log,
                            isNext = log.id == nextSetId,
                            restRemaining = if (waiting) restRemaining else null,
                            restFraction = if (waiting) restFraction else 0f,
                            onTap = { onTapSet(log) },
                            onLongPress = { onEditSet(log) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap a set when it's done · hold to change reps or weight",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = { showHowTo = !showHowTo }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (showHowTo) "Hide how to do it" else "How to do it")
                    Icon(if (showHowTo) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
                AnimatedVisibility(visible = showHowTo) {
                    Column { InstructionSteps(group.instructions) }
                }
            }
        }
    }
}

@Composable
private fun RestBar(remainingMs: Long, totalSeconds: Int, onAdd: () -> Unit, onSkip: () -> Unit) {
    val remaining = ((remainingMs + 999) / 1000).coerceAtLeast(0)
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("REST", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    String.format(Locale.US, "%d:%02d", remaining / 60, remaining % 60),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            OutlinedButton(onClick = onAdd, shape = PunkButtonShape) { Text("+30s") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onSkip) { Text("Skip") }
        }
        LinearProgressIndicator(
            progress = { (remainingMs.toFloat() / (totalSeconds * 1000f)).coerceIn(0f, 1f) },
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

/** Long-press on a bubble: record what you actually did, e.g. 8 reps instead of 10. */
@Composable
private fun SetEditDialog(log: SetLog, onDismiss: () -> Unit, onSave: (value: Int, weightKg: Double?) -> Unit) {
    val isTime = log.targetType == TargetType.TIME
    var value by remember { mutableStateOf(log.actualValue ?: log.targetValue) }
    // Weight in 0.5 kg units so the stepper can move in 2.5 kg plates.
    var halfKg by remember { mutableStateOf(((log.weightKg ?: 0.0) * 2).toInt()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${log.exerciseName} · set ${log.setNumber}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Planned: ${formatTarget(log.targetValue, log.targetType)}${if (isTime) "" else " reps"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Text(if (isTime) "Seconds held" else "Reps done", style = MaterialTheme.typography.labelLarge)
                Stepper(
                    value = value,
                    onValueChange = { value = it },
                    label = if (isTime) "seconds" else "reps",
                    range = if (isTime) 0..600 else 0..100,
                    step = if (isTime) 5 else 1,
                    buttonSize = 44.dp,
                )
                if (log.weightKg != null) {
                    Spacer(Modifier.height(16.dp))
                    Text("Weight (kg)", style = MaterialTheme.typography.labelLarge)
                    Stepper(
                        value = halfKg,
                        onValueChange = { halfKg = it },
                        label = "weight",
                        range = 0..1000,
                        step = 5,
                        display = { formatWeight(it / 2.0)!!.removeSuffix(" kg") },
                        buttonSize = 44.dp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value, log.weightKg?.let { halfKg / 2.0 }) }) { Text("Save as done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
