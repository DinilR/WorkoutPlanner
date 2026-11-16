package com.workoutplanner.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutplanner.data.db.SessionSummary
import com.workoutplanner.data.repo.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HistoryState(
    val sessions: List<SessionSummary>,
    /** Workouts finished on each day, for the heatmap. */
    val perDay: Map<LocalDate, Int>,
    val today: LocalDate,
) {
    val total get() = sessions.size

    /** Workouts since Monday of this week. */
    val thisWeek get() = perDay.filterKeys { !it.isBefore(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) }.values.sum()

    /** Consecutive days with a workout, ending today (or yesterday, if today isn't done yet). */
    val streak: Int get() {
        var day = if (today in perDay) today else today.minusDays(1)
        var count = 0
        while (day in perDay) {
            count++
            day = day.minusDays(1)
        }
        return count
    }
}

class HistoryViewModel(sessions: SessionRepository) : ViewModel() {

    /** Null until the first load. */
    val state: StateFlow<HistoryState?> = sessions.observeHistory()
        .map { list ->
            val zone = ZoneId.systemDefault()
            HistoryState(
                sessions = list,
                perDay = list.groupingBy { Instant.ofEpochMilli(it.finishedAt).atZone(zone).toLocalDate() }.eachCount(),
                today = LocalDate.now(zone),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
