package com.workoutplanner.data.repo

import androidx.room.withTransaction
import com.workoutplanner.data.db.AppDatabase
import com.workoutplanner.data.db.Rating
import com.workoutplanner.data.db.SetLog
import com.workoutplanner.data.db.WorkoutSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class SessionRepository(private val db: AppDatabase) {
    private val sessionDao = db.sessionDao()
    private val planDao = db.planDao()

    fun observeActiveSummary() = sessionDao.observeActiveSummary()
    fun observeSession(id: Long) = sessionDao.observeSession(id)
    fun observeSetLogs(sessionId: Long) = sessionDao.observeSetLogs(sessionId)
    fun observeHistory() = sessionDao.observeFinishedSummaries()

    /** What the user did for [exerciseId] last time, for the "Last time" hint. */
    suspend fun lastCompletedSets(exerciseId: Long, excludeSessionId: Long) =
        sessionDao.getLastCompletedSets(exerciseId, excludeSessionId)

    suspend fun getActiveSession() = sessionDao.getActiveSession()
    suspend fun getSetLogs(sessionId: Long) = sessionDao.getSetLogs(sessionId)

    /** Starts a workout: one [SetLog] per planned set, copied from the plan. */
    suspend fun startSession(planId: Long): Long = db.withTransaction {
        val plan = requireNotNull(planDao.getPlan(planId)) { "Plan $planId not found" }
        val sessionId = sessionDao.insertSession(
            WorkoutSession(planId = plan.id, planName = plan.name, startedAt = System.currentTimeMillis())
        )
        sessionDao.insertSetLogs(
            planDao.getRows(planId).flatMap { row ->
                (1..row.sets).map { setNumber ->
                    SetLog(
                        sessionId = sessionId,
                        exerciseId = row.exerciseId,
                        exerciseName = row.exerciseName,
                        exerciseOrder = row.orderIndex,
                        setNumber = setNumber,
                        targetType = row.targetType,
                        targetValue = row.target,
                        weightKg = row.weightKg,
                        restSeconds = row.restSeconds,
                    )
                }
            }
        )
        sessionId
    }

    /** Saved immediately on every tap, so closing the app loses nothing. */
    suspend fun completeSet(log: SetLog, actualValue: Int? = null, weightKg: Double? = log.weightKg) =
        sessionDao.updateSet(
            id = log.id,
            completedAt = log.completedAt ?: System.currentTimeMillis(),
            actualValue = actualValue ?: log.actualValue ?: log.targetValue,
            weightKg = weightKg,
        )

    suspend fun uncompleteSet(log: SetLog) =
        sessionDao.updateSet(id = log.id, completedAt = null, actualValue = log.actualValue, weightKg = log.weightKg)

    suspend fun finishSession(id: Long, rating: Rating?) =
        sessionDao.finishSession(id, System.currentTimeMillis(), rating)

    suspend fun discardSession(id: Long) = sessionDao.deleteSession(id)

    fun observeLastFinishedStart() = sessionDao.observeLastFinishedStart()
    suspend fun lastFinishedStart() = sessionDao.getLastFinishedStart()

    /** True if a workout was finished today, in the phone's time zone. */
    suspend fun trainedToday(): Boolean {
        val zone = ZoneId.systemDefault()
        val last = sessionDao.getLastFinishTime() ?: return false
        return Instant.ofEpochMilli(last).atZone(zone).toLocalDate() == LocalDate.now(zone)
    }

    /**
     * Consecutive days with a finished workout, ending today or yesterday.
     * [includeToday] counts the workout being finished right now.
     */
    suspend fun currentStreak(includeToday: Boolean): Int {
        val zone = ZoneId.systemDefault()
        val days = sessionDao.getFinishTimes()
            .map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            .toMutableSet()
        val today = LocalDate.now(zone)
        if (includeToday) days += today

        var day = if (today in days) today else today.minusDays(1)
        var streak = 0
        while (day in days) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }
}
