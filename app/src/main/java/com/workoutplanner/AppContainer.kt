package com.workoutplanner

import android.content.Context
import com.workoutplanner.data.db.AppDatabase
import com.workoutplanner.data.repo.PlanRepository
import com.workoutplanner.data.repo.SessionRepository
import com.workoutplanner.reminder.ReminderScheduler
import com.workoutplanner.sound.SoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual dependency wiring: one database, its repositories, the sound player and reminders for the whole app. */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val database = AppDatabase.build(context, appScope)

    val planRepository = PlanRepository(database)
    val sessionRepository = SessionRepository(database)
    val soundPlayer = SoundPlayer(context)
    val reminderScheduler = ReminderScheduler(context, sessionRepository, appScope)
}
