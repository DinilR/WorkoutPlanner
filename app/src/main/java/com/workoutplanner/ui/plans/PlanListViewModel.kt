package com.workoutplanner.ui.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.workoutplanner.data.db.ActiveSessionSummary
import com.workoutplanner.data.db.PlanSummary
import com.workoutplanner.data.repo.PlanRepository
import com.workoutplanner.data.repo.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlanListViewModel(
    private val plans: PlanRepository,
    sessions: SessionRepository,
) : ViewModel() {

    /** Null while the first load (or first-launch seeding) is in progress. */
    val plansState: StateFlow<List<PlanSummary>?> = plans.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeSession: StateFlow<ActiveSessionSummary?> = sessions.observeActiveSummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun duplicate(planId: Long) {
        viewModelScope.launch { plans.duplicatePlan(planId) }
    }

    fun delete(planId: Long) {
        viewModelScope.launch { plans.deletePlan(planId) }
    }
}
