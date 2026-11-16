package com.workoutplanner.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.workoutplanner.AppContainer
import com.workoutplanner.ui.editor.PlanEditorScreen
import com.workoutplanner.ui.editor.PlanEditorViewModel
import com.workoutplanner.ui.finish.FinishScreen
import com.workoutplanner.ui.finish.FinishViewModel
import com.workoutplanner.ui.history.HistoryScreen
import com.workoutplanner.ui.history.HistoryViewModel
import com.workoutplanner.ui.history.SessionDetailScreen
import com.workoutplanner.ui.history.SessionDetailViewModel
import com.workoutplanner.ui.plans.PlanDetailScreen
import com.workoutplanner.ui.plans.PlanDetailViewModel
import com.workoutplanner.ui.plans.PlanListScreen
import com.workoutplanner.ui.plans.PlanListViewModel
import com.workoutplanner.ui.workout.WorkoutScreen
import com.workoutplanner.ui.workout.WorkoutViewModel

private object Routes {
    const val PLANS = "plans"
    const val PLAN = "plan/{planId}"
    const val EDITOR = "editor?planId={planId}"
    const val WORKOUT = "workout/{sessionId}"
    const val FINISH = "finish/{sessionId}"
    const val HISTORY = "history"
    const val HISTORY_SESSION = "history/{sessionId}"

    fun plan(id: Long) = "plan/$id"
    fun editor(id: Long? = null) = if (id == null) "editor" else "editor?planId=$id"
    fun workout(id: Long) = "workout/$id"
    fun finish(id: Long) = "finish/$id"
    fun historySession(id: Long) = "history/$id"
}

@Composable
fun AppNavHost(container: AppContainer) {
    val nav = rememberNavController()
    val plans = container.planRepository
    val sessions = container.sessionRepository

    NavHost(navController = nav, startDestination = Routes.PLANS) {
        composable(Routes.PLANS) {
            PlanListScreen(
                viewModel = viewModel { PlanListViewModel(plans, sessions) },
                onOpenPlan = { nav.navigate(Routes.plan(it)) },
                onNewPlan = { nav.navigate(Routes.editor()) },
                onEditPlan = { nav.navigate(Routes.editor(it)) },
                onResume = { nav.navigate(Routes.workout(it)) },
                onOpenHistory = { nav.navigate(Routes.HISTORY) },
                soundPlayer = container.soundPlayer,
                reminderScheduler = container.reminderScheduler,
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                viewModel = viewModel { HistoryViewModel(sessions) },
                onBack = { nav.popBackStack() },
                onOpenSession = { nav.navigate(Routes.historySession(it)) },
            )
        }
        composable(Routes.HISTORY_SESSION, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
            val sessionId = entry.arguments!!.getLong("sessionId")
            SessionDetailScreen(
                viewModel = viewModel { SessionDetailViewModel(sessionId, sessions) },
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.PLAN, arguments = listOf(navArgument("planId") { type = NavType.LongType })) { entry ->
            val planId = entry.arguments!!.getLong("planId")
            PlanDetailScreen(
                viewModel = viewModel { PlanDetailViewModel(planId, plans, sessions) },
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(Routes.editor(planId)) },
                onOpenPlan = { nav.navigate(Routes.plan(it)) { popUpTo(Routes.PLANS) } },
                onWorkoutStarted = { nav.navigate(Routes.workout(it)) { popUpTo(Routes.PLANS) } },
            )
        }
        composable(
            Routes.EDITOR,
            arguments = listOf(navArgument("planId") { type = NavType.LongType; defaultValue = -1L }),
        ) { entry ->
            val planId = entry.arguments!!.getLong("planId").takeIf { it > 0 }
            PlanEditorScreen(
                viewModel = viewModel { PlanEditorViewModel(planId, plans) },
                onBack = { nav.popBackStack() },
                onSaved = { savedId ->
                    nav.navigate(Routes.plan(savedId)) { popUpTo(Routes.PLANS) }
                },
            )
        }
        composable(Routes.WORKOUT, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
            val sessionId = entry.arguments!!.getLong("sessionId")
            WorkoutScreen(
                viewModel = viewModel { WorkoutViewModel(sessionId, sessions, container.soundPlayer) },
                onBack = { nav.popBackStack() },
                onFinish = { nav.navigate(Routes.finish(sessionId)) },
                onDiscarded = { nav.popBackStack(Routes.PLANS, inclusive = false) },
                soundPlayer = container.soundPlayer,
            )
        }
        composable(Routes.FINISH, arguments = listOf(navArgument("sessionId") { type = NavType.LongType })) { entry ->
            val sessionId = entry.arguments!!.getLong("sessionId")
            FinishScreen(
                viewModel = viewModel { FinishViewModel(sessionId, sessions) },
                onBack = { nav.popBackStack() },
                onSaved = { nav.popBackStack(Routes.PLANS, inclusive = false) },
            )
        }
    }
}
