package com.workoutplanner.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutplanner.data.db.Exercise
import com.workoutplanner.data.db.Level
import com.workoutplanner.data.db.TargetType
import com.workoutplanner.data.repo.PlanRepository
import com.workoutplanner.data.repo.PlanRowInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One editable row of the plan table. [key] keeps rows stable while they're reordered. */
data class EditorRow(
    val key: Long,
    val exerciseName: String = "",
    /** Only used when [exerciseName] is a new exercise. */
    val muscleGroup: String = "",
    /** Only used when [exerciseName] is a new exercise. One step per line. */
    val instructions: String = "",
    val sets: Int = 3,
    val target: Int = 10,
    val targetType: TargetType = TargetType.REPS,
    val weightText: String = "",
    val restSeconds: Int = 60,
) {
    val exerciseError get() = exerciseName.isBlank()
    val weightError get() = weightText.isNotBlank() && weightText.replace(',', '.').toDoubleOrNull()?.takeIf { it >= 0 } == null
}

data class EditorState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val name: String = "",
    val description: String = "",
    val level: Level = Level.BEGINNER,
    val rows: List<EditorRow> = emptyList(),
    /** Errors are only highlighted after the first save attempt. */
    val showErrors: Boolean = false,
    val saving: Boolean = false,
) {
    val nameError get() = name.isBlank()
    val isValid get() = !nameError && rows.isNotEmpty() && rows.none { it.exerciseError || it.weightError }
}

class PlanEditorViewModel(
    private val planId: Long?,
    private val plans: PlanRepository,
) : ViewModel() {

    private var nextKey = 0L
    private val _state = MutableStateFlow(EditorState())
    val state = _state.asStateFlow()

    /** The exercise library, for autocomplete in the exercise cell. */
    val exercises: StateFlow<List<Exercise>> = plans.observeExercises()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (planId == null) {
                _state.value = EditorState(loading = false, isNew = true, rows = listOf(EditorRow(key = nextKey++)))
                return@launch
            }
            val plan = plans.getPlan(planId)
            val rows = plans.getRows(planId)
            _state.value = EditorState(
                loading = false,
                isNew = false,
                name = plan?.name.orEmpty(),
                description = plan?.description.orEmpty(),
                level = plan?.level ?: Level.BEGINNER,
                rows = rows.map { row ->
                    EditorRow(
                        key = nextKey++,
                        exerciseName = row.exerciseName,
                        sets = row.sets,
                        target = row.target,
                        targetType = row.targetType,
                        weightText = row.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty(),
                        restSeconds = row.restSeconds,
                    )
                },
            )
        }
    }

    fun setName(name: String) = _state.update { it.copy(name = name) }
    fun setDescription(description: String) = _state.update { it.copy(description = description) }
    fun setLevel(level: Level) = _state.update { it.copy(level = level) }

    /** New rows copy the rest time of the row above, which is usually what people want. */
    fun addRow() = _state.update { s ->
        val previousRest = s.rows.lastOrNull()?.restSeconds ?: 60
        s.copy(rows = s.rows + EditorRow(key = nextKey++, restSeconds = previousRest))
    }

    fun updateRow(key: Long, transform: (EditorRow) -> EditorRow) = _state.update { s ->
        s.copy(rows = s.rows.map { if (it.key == key) transform(it) else it })
    }

    /** Switching between reps and seconds picks a sensible starting value. */
    fun toggleTargetType(key: Long) = updateRow(key) { row ->
        if (row.targetType == TargetType.REPS) row.copy(targetType = TargetType.TIME, target = 30)
        else row.copy(targetType = TargetType.REPS, target = 10)
    }

    fun moveRow(key: Long, delta: Int) = _state.update { s ->
        val from = s.rows.indexOfFirst { it.key == key }
        val to = from + delta
        if (from < 0 || to !in s.rows.indices) return@update s
        s.copy(rows = s.rows.toMutableList().apply { add(to, removeAt(from)) })
    }

    /** Returns what was removed, so the screen can offer Undo. */
    fun removeRow(key: Long): Pair<Int, EditorRow>? {
        val index = _state.value.rows.indexOfFirst { it.key == key }
        if (index < 0) return null
        val row = _state.value.rows[index]
        _state.update { it.copy(rows = it.rows - row) }
        return index to row
    }

    fun restoreRow(index: Int, row: EditorRow) = _state.update { s ->
        s.copy(rows = s.rows.toMutableList().apply { add(index.coerceIn(0, size), row) })
    }

    /** Returns false (and highlights the problems) if the table isn't valid yet. */
    fun save(onSaved: (Long) -> Unit): Boolean {
        val s = _state.value
        if (!s.isValid) {
            _state.update { it.copy(showErrors = true) }
            return false
        }
        if (s.saving) return true
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = plans.savePlan(
                planId = planId,
                name = s.name.trim(),
                description = s.description.trim().ifBlank { null },
                level = s.level,
                rows = s.rows.map { row ->
                    PlanRowInput(
                        exerciseName = row.exerciseName.trim(),
                        muscleGroup = row.muscleGroup,
                        instructions = row.instructions,
                        sets = row.sets,
                        target = row.target,
                        targetType = row.targetType,
                        weightKg = row.weightText.replace(',', '.').toDoubleOrNull(),
                        restSeconds = row.restSeconds,
                    )
                },
            )
            onSaved(id)
        }
        return true
    }
}
