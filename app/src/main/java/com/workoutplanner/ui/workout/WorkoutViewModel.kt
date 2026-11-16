package com.workoutplanner.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutplanner.data.db.SetLog
import com.workoutplanner.data.db.TargetType
import com.workoutplanner.data.db.WorkoutSession
import com.workoutplanner.data.repo.SessionRepository
import com.workoutplanner.sound.SoundPlayer
import com.workoutplanner.sound.WorkoutSound
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** All sets of one exercise in this workout. */
data class ExerciseGroup(
    val exerciseId: Long,
    val order: Int,
    val name: String,
    val instructions: String?,
    val targetType: TargetType,
    val restSeconds: Int,
    val sets: List<SetLog>,
) {
    val doneCount get() = sets.count { it.completedAt != null }
    val isDone get() = sets.all { it.completedAt != null }
    val volumeKg get() = sets.sumOf { it.volumeKg }
}

/** Reps × kg for a completed, weighted rep set; 0 otherwise. */
val SetLog.volumeKg: Double
    get() = if (completedAt == null || targetType != TargetType.REPS) 0.0
    else (actualValue ?: targetValue) * (weightKg ?: 0.0)

data class WorkoutUiState(val session: WorkoutSession, val groups: List<ExerciseGroup>) {
    val total get() = groups.sumOf { it.sets.size }
    val done get() = groups.sumOf { it.doneCount }
    val volumeKg get() = groups.sumOf { it.volumeKg }
    val allDone get() = total > 0 && done == total

    /** The first exercise that still has sets left; its card is expanded. */
    val currentIndex get() = groups.indexOfFirst { !it.isDone }
}

/** A running rest countdown, shown on the bubble of [nextSetId]. */
data class RestState(val nextSetId: Long, val endsAt: Long, val totalSeconds: Int)

class WorkoutViewModel(
    private val sessionId: Long,
    private val sessions: SessionRepository,
    private val sounds: SoundPlayer,
) : ViewModel() {

    val state: StateFlow<WorkoutUiState?> =
        combine(sessions.observeSession(sessionId), sessions.observeSetLogs(sessionId)) { session, logs ->
            session?.let {
                WorkoutUiState(
                    session = it,
                    groups = logs.groupBy { row -> row.log.exerciseOrder }.map { (order, rows) ->
                        val first = rows.first()
                        ExerciseGroup(
                            exerciseId = first.log.exerciseId,
                            order = order,
                            name = first.log.exerciseName,
                            instructions = first.instructions,
                            targetType = first.log.targetType,
                            restSeconds = first.log.restSeconds,
                            sets = rows.map { row -> row.log },
                        )
                    },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _rest = MutableStateFlow<RestState?>(null)
    val rest = _rest.asStateFlow()

    /** What was done for each exercise in the most recent earlier workout, keyed by exercise id. */
    val lastTime: StateFlow<Map<Long, List<SetLog>>> = state
        .map { it?.groups?.map { group -> group.exerciseId }?.distinct() }
        .filterNotNull()
        .distinctUntilChanged()
        .map { ids ->
            ids.associateWith { sessions.lastCompletedSets(it, excludeSessionId = sessionId) }
                .filterValues { it.isNotEmpty() }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Called by the screen once per second of rest. */
    fun restTick(secondsLeft: Int) = sounds.restTick(secondsLeft)

    /** Tap on an empty bubble. Saved straight away, then a sound plays and the rest timer starts. */
    fun completeSet(log: SetLog, actualValue: Int? = null, weightKg: Double? = log.weightKg) {
        val wasDone = log.completedAt != null
        viewModelScope.launch { sessions.completeSet(log, actualValue, weightKg) }
        if (!wasDone) {
            playCompletionSound(log)
            startRestAfter(log)
        }
    }

    /** One sound per tap; the biggest milestone wins (workout > exercise > set). */
    private fun playCompletionSound(log: SetLog) {
        val allSets = state.value?.groups?.flatMap { it.sets } ?: return
        val remaining = allSets.filter { it.completedAt == null && it.id != log.id }
        sounds.play(
            when {
                remaining.isEmpty() -> WorkoutSound.WORKOUT_DONE
                remaining.none { it.exerciseOrder == log.exerciseOrder } -> WorkoutSound.EXERCISE_DONE
                else -> WorkoutSound.SET_DONE
            }
        )
    }

    /** Tap on a filled bubble. */
    fun undoSet(log: SetLog) {
        viewModelScope.launch { sessions.uncompleteSet(log) }
    }

    fun addRestTime(seconds: Int) {
        _rest.value = _rest.value?.let { it.copy(endsAt = it.endsAt + seconds * 1000L, totalSeconds = it.totalSeconds + seconds) }
    }

    fun skipRest() {
        _rest.value = null
    }

    /** Called by the screen when the countdown reaches zero. Skipping rest makes no sound. */
    fun restFinished(finished: RestState) {
        if (_rest.value == finished) {
            _rest.value = null
            sounds.play(WorkoutSound.REST_OVER)
        }
    }

    fun discard(onDiscarded: () -> Unit) {
        viewModelScope.launch {
            sessions.discardSession(sessionId)
            onDiscarded()
        }
    }

    private fun startRestAfter(log: SetLog) {
        val allSets = state.value?.groups?.flatMap { it.sets } ?: return
        val index = allSets.indexOfFirst { it.id == log.id }
        // Next undone set after this one, wrapping round to any skipped earlier sets.
        val next = (allSets.drop(index + 1) + allSets.take(index))
            .firstOrNull { it.completedAt == null && it.id != log.id }
        _rest.value = next?.let {
            RestState(nextSetId = it.id, endsAt = System.currentTimeMillis() + log.restSeconds * 1000L, totalSeconds = log.restSeconds)
        }
    }
}
