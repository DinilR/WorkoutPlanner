package com.workoutplanner.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): Exercise?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(exercise: Exercise): Long
}

@Dao
interface PlanDao {
    @Query(
        """
        SELECT p.*,
               COUNT(pe.id) AS exerciseCount,
               COALESCE(SUM(pe.sets), 0) AS totalSets,
               COALESCE(SUM(pe.sets * pe.restSeconds), 0) AS totalRestSeconds
        FROM plans p
        LEFT JOIN plan_exercises pe ON pe.planId = p.id
        GROUP BY p.id
        ORDER BY p.isDefault DESC, p.name COLLATE NOCASE
        """
    )
    fun observeSummaries(): Flow<List<PlanSummary>>

    @Query("SELECT * FROM plans WHERE id = :id")
    fun observePlan(id: Long): Flow<WorkoutPlan?>

    @Query("SELECT * FROM plans WHERE id = :id")
    suspend fun getPlan(id: Long): WorkoutPlan?

    @Query("SELECT * FROM plans WHERE isDefault = 1 AND name = :name LIMIT 1")
    suspend fun findDefaultPlan(name: String): WorkoutPlan?

    @Query(
        """
        SELECT pe.id, pe.exerciseId, pe.orderIndex, pe.sets, pe.target, pe.targetType,
               pe.weightKg, pe.restSeconds,
               e.name AS exerciseName, e.muscleGroup, e.instructions
        FROM plan_exercises pe
        JOIN exercises e ON e.id = pe.exerciseId
        WHERE pe.planId = :planId
        ORDER BY pe.orderIndex
        """
    )
    fun observeRows(planId: Long): Flow<List<PlanRow>>

    @Query(
        """
        SELECT pe.id, pe.exerciseId, pe.orderIndex, pe.sets, pe.target, pe.targetType,
               pe.weightKg, pe.restSeconds,
               e.name AS exerciseName, e.muscleGroup, e.instructions
        FROM plan_exercises pe
        JOIN exercises e ON e.id = pe.exerciseId
        WHERE pe.planId = :planId
        ORDER BY pe.orderIndex
        """
    )
    suspend fun getRows(planId: Long): List<PlanRow>

    @Insert
    suspend fun insertPlan(plan: WorkoutPlan): Long

    @Update
    suspend fun updatePlan(plan: WorkoutPlan)

    @Delete
    suspend fun deletePlan(plan: WorkoutPlan)

    @Insert
    suspend fun insertRows(rows: List<PlanExercise>)

    @Query("DELETE FROM plan_exercises WHERE planId = :planId")
    suspend fun deleteRows(planId: Long)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insertSession(session: WorkoutSession): Long

    @Insert
    suspend fun insertSetLogs(logs: List<SetLog>)

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getActiveSession(): WorkoutSession?

    @Query(
        """
        SELECT s.id AS sessionId, s.planName,
               SUM(CASE WHEN l.completedAt IS NOT NULL THEN 1 ELSE 0 END) AS done,
               COUNT(l.id) AS total
        FROM sessions s
        JOIN set_logs l ON l.sessionId = s.id
        WHERE s.finishedAt IS NULL
        GROUP BY s.id
        ORDER BY s.startedAt DESC
        LIMIT 1
        """
    )
    fun observeActiveSummary(): Flow<ActiveSessionSummary?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<WorkoutSession?>

    @Query(
        """
        SELECT l.*, e.instructions
        FROM set_logs l
        LEFT JOIN exercises e ON e.id = l.exerciseId
        WHERE l.sessionId = :sessionId
        ORDER BY l.exerciseOrder, l.setNumber
        """
    )
    fun observeSetLogs(sessionId: Long): Flow<List<SetLogWithInfo>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId")
    suspend fun getSetLogs(sessionId: Long): List<SetLog>

    @Query(
        """
        UPDATE set_logs
        SET completedAt = :completedAt, actualValue = :actualValue, weightKg = :weightKg
        WHERE id = :id
        """
    )
    suspend fun updateSet(id: Long, completedAt: Long?, actualValue: Int?, weightKg: Double?)

    @Query("UPDATE sessions SET finishedAt = :finishedAt, rating = :rating WHERE id = :id")
    suspend fun finishSession(id: Long, finishedAt: Long, rating: Rating?)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("SELECT finishedAt FROM sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    suspend fun getFinishTimes(): List<Long>

    /** When the most recently finished workout started. Drives the smart reminder. */
    @Query("SELECT startedAt FROM sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC LIMIT 1")
    fun observeLastFinishedStart(): Flow<Long?>

    @Query("SELECT startedAt FROM sessions WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC LIMIT 1")
    suspend fun getLastFinishedStart(): Long?

    @Query("SELECT MAX(finishedAt) FROM sessions")
    suspend fun getLastFinishTime(): Long?

    /** Finished workouts, newest first. Volume counts completed rep sets only (reps × kg). */
    @Query(
        """
        SELECT s.id AS sessionId, s.planName, s.startedAt, s.finishedAt, s.rating,
               COALESCE(SUM(CASE WHEN l.completedAt IS NOT NULL THEN 1 ELSE 0 END), 0) AS setsDone,
               COUNT(l.id) AS totalSets,
               COALESCE(SUM(CASE WHEN l.completedAt IS NOT NULL AND l.targetType = 'REPS'
                   THEN COALESCE(l.actualValue, l.targetValue) * COALESCE(l.weightKg, 0) ELSE 0 END), 0) AS volumeKg
        FROM sessions s
        LEFT JOIN set_logs l ON l.sessionId = s.id
        WHERE s.finishedAt IS NOT NULL
        GROUP BY s.id
        ORDER BY s.finishedAt DESC
        """
    )
    fun observeFinishedSummaries(): Flow<List<SessionSummary>>

    /** The completed sets of [exerciseId] from the most recent finished workout that included it. */
    @Query(
        """
        SELECT l.* FROM set_logs l
        WHERE l.exerciseId = :exerciseId AND l.completedAt IS NOT NULL AND l.sessionId = (
            SELECT s.id FROM sessions s
            JOIN set_logs l2 ON l2.sessionId = s.id
            WHERE l2.exerciseId = :exerciseId AND l2.completedAt IS NOT NULL
              AND s.finishedAt IS NOT NULL AND s.id != :excludeSessionId
            ORDER BY s.finishedAt DESC
            LIMIT 1
        )
        ORDER BY l.setNumber
        """
    )
    suspend fun getLastCompletedSets(exerciseId: Long, excludeSessionId: Long): List<SetLog>
}
