package com.workoutplanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutplanner.data.repo.SessionRepository
import com.workoutplanner.ui.workout.ExerciseGroup
import com.workoutplanner.ui.workout.WorkoutUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One past workout, grouped by exercise the same way the workout screen shows it. */
class SessionDetailViewModel(
    private val sessionId: Long,
    private val sessions: SessionRepository,
) : ViewModel() {

    val state: StateFlow<WorkoutUiState?> =
        combine(sessions.observeSession(sessionId), sessions.observeSetLogs(sessionId)) { session, rows ->
            session?.let {
                WorkoutUiState(
                    session = it,
                    groups = rows.groupBy { row -> row.log.exerciseOrder }.map { (order, group) ->
                        val first = group.first()
                        ExerciseGroup(
                            exerciseId = first.log.exerciseId,
                            order = order,
                            name = first.log.exerciseName,
                            instructions = first.instructions,
                            targetType = first.log.targetType,
                            restSeconds = first.log.restSeconds,
                            sets = group.map { row -> row.log },
                        )
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            sessions.discardSession(sessionId)
            onDeleted()
        }
    }
}
