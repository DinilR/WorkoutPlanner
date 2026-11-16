package com.workoutplanner.ui.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.data.db.Exercise
import com.workoutplanner.data.db.Level
import com.workoutplanner.data.db.TargetType
import com.workoutplanner.ui.components.GradientTopBar
import com.workoutplanner.ui.components.Stepper
import com.workoutplanner.ui.theme.LocalWorkoutColors
import com.workoutplanner.ui.theme.PunkButtonShape
import com.workoutplanner.ui.formatRest
import com.workoutplanner.ui.label
import kotlinx.coroutines.launch

private val RestOptions = listOf(30, 45, 60, 90, 120, 180)

private val HeaderHeight = 40.dp
private val RowHeight = 76.dp
private val ExerciseWidth = 140.dp
private val SetsWidth = 116.dp
private val RepsWidth = 116.dp
private val KgWidth = 88.dp
private val RestWidth = 92.dp
private val ActionsWidth = 128.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    viewModel: PlanEditorViewModel,
    onBack: () -> Unit,
    onSaved: (planId: Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val library by viewModel.exercises.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pickingRowKey by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = { Text(if (state.isNew) "NEW PLAN" else "EDIT PLAN") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close without saving") }
                },
                actions = {
                    Button(
                        enabled = !state.loading && !state.saving,
                        shape = PunkButtonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LocalWorkoutColors.current.done,
                            contentColor = LocalWorkoutColors.current.onDone,
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            val ok = viewModel.save(onSaved)
                            if (!ok) scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(
                                    if (state.rows.isEmpty()) "Add at least one exercise row"
                                    else "Fill in the highlighted cells"
                                )
                            }
                        },
                    ) { Text("SAVE", style = MaterialTheme.typography.labelLarge) }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text("Plan name") },
                singleLine = true,
                isError = state.showErrors && state.nameError,
                supportingText = if (state.showErrors && state.nameError) {
                    { Text("Give your plan a name") }
                } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.description,
                onValueChange = viewModel::setDescription,
                label = { Text("Description (optional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text("Level", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Level.entries.forEach { level ->
                    FilterChip(
                        selected = state.level == level,
                        onClick = { viewModel.setLevel(level) },
                        label = { Text(level.label) },
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("EXERCISES", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(
                "Tap an exercise cell to choose or type a name. Scroll the table sideways for more columns.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            PlanTable(
                state = state,
                onPickExercise = { pickingRowKey = it },
                onRowChange = viewModel::updateRow,
                onToggleType = viewModel::toggleTargetType,
                onMove = viewModel::moveRow,
                onDelete = { key ->
                    val removed = viewModel.removeRow(key) ?: return@PlanTable
                    scope.launch {
                        snackbar.currentSnackbarData?.dismiss()
                        val result = snackbar.showSnackbar(
                            message = "Removed ${removed.second.exerciseName.ifBlank { "row" }}",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short,
                        )
                        if (result == SnackbarResult.ActionPerformed) viewModel.restoreRow(removed.first, removed.second)
                    }
                },
            )

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = viewModel::addRow,
                shape = PunkButtonShape,
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add exercise row")
            }
            Spacer(Modifier.height(80.dp))
        }
    }

    val pickingRow = state.rows.firstOrNull { it.key == pickingRowKey }
    if (pickingRow != null) {
        ExercisePickerDialog(
            initial = pickingRow,
            library = library,
            onDismiss = { pickingRowKey = null },
            onConfirm = { name, muscle, instructions ->
                viewModel.updateRow(pickingRow.key) {
                    it.copy(exerciseName = name, muscleGroup = muscle, instructions = instructions)
                }
                pickingRowKey = null
            },
        )
    }
}

/**
 * The spreadsheet-style table. The Exercise column stays pinned on the left while the
 * number columns scroll sideways on narrow screens. Fixed row heights keep both halves aligned.
 */
@Composable
private fun PlanTable(
    state: EditorState,
    onPickExercise: (Long) -> Unit,
    onRowChange: (Long, (EditorRow) -> EditorRow) -> Unit,
    onToggleType: (Long) -> Unit,
    onMove: (Long, Int) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    val headerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val stripeColor = MaterialTheme.colorScheme.surfaceContainer

    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(BorderStroke(1.dp, outline), MaterialTheme.shapes.medium)
    ) {
        // Pinned column
        Column(Modifier.width(ExerciseWidth)) {
            HeaderCell("Exercise", ExerciseWidth, headerColor, TextAlign.Start)
            state.rows.forEachIndexed { index, row ->
                HorizontalDivider(color = outline)
                ExerciseCell(
                    row = row,
                    showError = state.showErrors && row.exerciseError,
                    background = if (index % 2 == 1) stripeColor else Color.Transparent,
                    onClick = { onPickExercise(row.key) },
                )
            }
        }
        Box(Modifier.width(1.dp).height(HeaderHeight + (RowHeight + 1.dp) * state.rows.size).background(outline))
        // Scrollable columns
        Column(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
            Row {
                HeaderCell("Sets", SetsWidth, headerColor)
                HeaderCell("Reps / Time", RepsWidth, headerColor)
                HeaderCell("kg", KgWidth, headerColor)
                HeaderCell("Rest", RestWidth, headerColor)
                HeaderCell("", ActionsWidth, headerColor)
            }
            state.rows.forEachIndexed { index, row ->
                HorizontalDivider(color = outline, modifier = Modifier.width(SetsWidth + RepsWidth + KgWidth + RestWidth + ActionsWidth))
                Row(
                    Modifier
                        .height(RowHeight)
                        .background(if (index % 2 == 1) stripeColor else Color.Transparent),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.width(SetsWidth), contentAlignment = Alignment.Center) {
                        Stepper(
                            value = row.sets,
                            onValueChange = { v -> onRowChange(row.key) { it.copy(sets = v) } },
                            label = "sets",
                            range = 1..10,
                        )
                    }
                    Column(Modifier.width(RepsWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                        val isTime = row.targetType == TargetType.TIME
                        Stepper(
                            value = row.target,
                            onValueChange = { v -> onRowChange(row.key) { it.copy(target = v) } },
                            label = if (isTime) "seconds" else "reps",
                            range = if (isTime) 5..600 else 1..100,
                            step = if (isTime) 5 else 1,
                        )
                        Text(
                            if (isTime) "seconds ⇄" else "reps ⇄",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onToggleType(row.key) }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    Box(Modifier.width(KgWidth).padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                        OutlinedTextField(
                            value = row.weightText,
                            onValueChange = { v -> onRowChange(row.key) { it.copy(weightText = v.take(6)) } },
                            placeholder = { Text("—") },
                            singleLine = true,
                            isError = row.weightError,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.Center),
                        )
                    }
                    Box(Modifier.width(RestWidth), contentAlignment = Alignment.Center) {
                        RestPicker(row.restSeconds) { v -> onRowChange(row.key) { it.copy(restSeconds = v) } }
                    }
                    Row(Modifier.width(ActionsWidth), horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { onMove(row.key, -1) }, enabled = index > 0, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(onClick = { onMove(row.key, 1) }, enabled = index < state.rows.lastIndex, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                        }
                        IconButton(onClick = { onDelete(row.key) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete row", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, width: Dp, color: Color, align: TextAlign = TextAlign.Center) {
    Box(
        Modifier.width(width).height(HeaderHeight).background(color).padding(horizontal = 12.dp),
        contentAlignment = if (align == TextAlign.Start) Alignment.CenterStart else Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExerciseCell(row: EditorRow, showError: Boolean, background: Color, onClick: () -> Unit) {
    val errorColor = MaterialTheme.colorScheme.error
    Box(
        Modifier
            .width(ExerciseWidth)
            .height(RowHeight)
            .background(background)
            .clickable(onClick = onClick)
            .padding(6.dp)
            .then(if (showError) Modifier.border(2.dp, errorColor, RoundedCornerShape(8.dp)) else Modifier)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                row.exerciseName.ifBlank { "Tap to choose" },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (row.exerciseName.isBlank()) FontWeight.Normal else FontWeight.Medium,
                color = when {
                    showError -> errorColor
                    row.exerciseName.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RestPicker(seconds: Int, onChange: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Text(formatRest(seconds))
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change rest time")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            RestOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(formatRest(option), fontWeight = if (option == seconds) FontWeight.Bold else null) },
                    onClick = { onChange(option); open = false },
                )
            }
        }
    }
}

/**
 * Pick an exercise from the library, or type a new one. New exercises can get a muscle
 * group and how-to steps, which are saved to the library with the plan.
 */
@Composable
private fun ExercisePickerDialog(
    initial: EditorRow,
    library: List<Exercise>,
    onDismiss: () -> Unit,
    onConfirm: (name: String, muscleGroup: String, instructions: String) -> Unit,
) {
    var query by remember { mutableStateOf(initial.exerciseName) }
    var muscle by remember { mutableStateOf(initial.muscleGroup) }
    var instructions by remember { mutableStateOf(initial.instructions) }

    val trimmed = query.trim()
    val exactMatch = library.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
    val suggestions = library
        .filter { trimmed.isEmpty() || it.name.contains(trimmed, ignoreCase = true) }
        .filter { it != exactMatch }
        .take(8)
    val isNew = trimmed.isNotEmpty() && exactMatch == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exercise") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    supportingText = {
                        Text(
                            when {
                                exactMatch != null -> "From your exercise library"
                                isNew -> "New exercise - it will be added to your library"
                                else -> "Pick one below or type a new name"
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                suggestions.forEach { exercise ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onConfirm(exercise.name, "", "") }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                    ) {
                        Text(exercise.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        exercise.muscleGroup?.let {
                            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (isNew) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = muscle,
                        onValueChange = { muscle = it },
                        label = { Text("Muscle group (optional)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("How to do it (optional)") },
                        placeholder = { Text("One step per line") },
                        minLines = 3,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = trimmed.isNotEmpty(),
                onClick = {
                    if (exactMatch != null) onConfirm(exactMatch.name, "", "") else onConfirm(trimmed, muscle, instructions)
                },
            ) { Text("Done") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
