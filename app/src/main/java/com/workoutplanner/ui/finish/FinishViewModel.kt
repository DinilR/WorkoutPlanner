package com.workoutplanner.ui.finish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutplanner.data.db.Rating
import com.workoutplanner.data.repo.SessionRepository
import com.workoutplanner.ui.workout.volumeKg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FinishSummary(
    val planName: String,
    val startedAt: Long,
    val setsDone: Int,
    val totalSets: Int,
    val volumeKg: Double,
) {
    val allDone get() = setsDone == totalSets
}

class FinishViewModel(
    private val sessionId: Long,
    private val sessions: SessionRepository,
) : ViewModel() {

    val summary: StateFlow<FinishSummary?> =
        combine(sessions.observeSession(sessionId), sessions.observeSetLogs(sessionId)) { session, rows ->
            session?.let {
                val logs = rows.map { row -> row.log }
                FinishSummary(
                    planName = it.planName,
                    startedAt = it.startedAt,
                    setsDone = logs.count { log -> log.completedAt != null },
                    totalSets = logs.size,
                    volumeKg = logs.sumOf { log -> log.volumeKg },
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Streak including the workout being finished now. */
    private val _streak = MutableStateFlow<Int?>(null)
    val streak = _streak.asStateFlow()

    private val _rating = MutableStateFlow<Rating?>(null)
    val rating = _rating.asStateFlow()

    private var saving = false

    init {
        viewModelScope.launch { _streak.value = sessions.currentStreak(includeToday = true) }
    }

    fun setRating(rating: Rating) {
        _rating.value = rating
    }

    fun save(onSaved: () -> Unit) {
        if (saving) return
        saving = true
        viewModelScope.launch {
            sessions.finishSession(sessionId, _rating.value)
            onSaved()
        }
    }
}
