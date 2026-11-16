package com.workoutplanner.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutplanner.data.db.ActiveSessionSummary
import com.workoutplanner.data.db.PlanRow
import com.workoutplanner.data.db.WorkoutPlan
import com.workoutplanner.data.repo.PlanRepository
import com.workoutplanner.data.repo.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanDetailState(val plan: WorkoutPlan?, val rows: List<PlanRow>) {
    val totalSets get() = rows.sumOf { it.sets }
    val estimatedMinutes get() = ((totalSets * 40 + rows.sumOf { it.sets * it.restSeconds }) / 60).coerceAtLeast(1)
}

class PlanDetailViewModel(
    private val planId: Long,
    private val plans: PlanRepository,
    private val sessions: SessionRepository,
) : ViewModel() {

    val state: StateFlow<PlanDetailState?> =
        combine(plans.observePlan(planId), plans.observeRows(planId)) { plan, rows -> PlanDetailState(plan, rows) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Set when the user taps Start while another workout is still unfinished. */
    private val _conflict = MutableStateFlow<ActiveSessionSummary?>(null)
    val conflict = _conflict.asStateFlow()

    fun start(onStarted: (sessionId: Long) -> Unit) {
        viewModelScope.launch {
            val active = sessions.observeActiveSummary().first()
            if (active != null) {
                _conflict.value = active
            } else {
                onStarted(sessions.startSession(planId))
            }
        }
    }

    fun resumeExisting(onResume: (sessionId: Long) -> Unit) {
        val active = _conflict.value ?: return
        _conflict.value = null
        onResume(active.sessionId)
    }

    fun discardAndStart(onStarted: (sessionId: Long) -> Unit) {
        val active = _conflict.value ?: return
        _conflict.value = null
        viewModelScope.launch {
            sessions.discardSession(active.sessionId)
            onStarted(sessions.startSession(planId))
        }
    }

    fun dismissConflict() {
        _conflict.value = null
    }

    fun duplicate(onCreated: (planId: Long) -> Unit) {
        viewModelScope.launch { onCreated(plans.duplicatePlan(planId)) }
    }
}
