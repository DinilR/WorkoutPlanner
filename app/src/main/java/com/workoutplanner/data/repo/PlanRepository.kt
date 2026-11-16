package com.workoutplanner.data.repo

import androidx.room.withTransaction
import com.workoutplanner.data.db.AppDatabase
import com.workoutplanner.data.db.Exercise
import com.workoutplanner.data.db.Level
import com.workoutplanner.data.db.PlanExercise
import com.workoutplanner.data.db.TargetType
import com.workoutplanner.data.db.WorkoutPlan

/** A row from the plan editor table, before it is matched to an exercise id. */
data class PlanRowInput(
    val exerciseName: String,
    val muscleGroup: String?,
    val instructions: String?,
    val sets: Int,
    val target: Int,
    val targetType: TargetType,
    val weightKg: Double?,
    val restSeconds: Int,
)

class PlanRepository(private val db: AppDatabase) {
    private val planDao = db.planDao()
    private val exerciseDao = db.exerciseDao()

    fun observeSummaries() = planDao.observeSummaries()
    fun observePlan(id: Long) = planDao.observePlan(id)
    fun observeRows(planId: Long) = planDao.observeRows(planId)
    fun observeExercises() = exerciseDao.observeAll()

    suspend fun getPlan(id: Long) = planDao.getPlan(id)
    suspend fun getRows(planId: Long) = planDao.getRows(planId)

    /**
     * Creates or updates a user plan from the editor table. Exercise names that aren't in
     * the library yet are added to it. Returns the plan id.
     */
    suspend fun savePlan(
        planId: Long?,
        name: String,
        description: String?,
        level: Level,
        rows: List<PlanRowInput>,
    ): Long = db.withTransaction {
        val id = if (planId == null) {
            planDao.insertPlan(WorkoutPlan(name = name, description = description, level = level))
        } else {
            val existing = requireNotNull(planDao.getPlan(planId)) { "Plan $planId not found" }
            require(!existing.isDefault) { "Default plans can't be edited" }
            planDao.updatePlan(existing.copy(name = name, description = description, level = level))
            planDao.deleteRows(planId)
            planId
        }
        planDao.insertRows(
            rows.mapIndexed { index, row ->
                PlanExercise(
                    planId = id,
                    exerciseId = exerciseIdFor(row),
                    orderIndex = index,
                    sets = row.sets,
                    target = row.target,
                    targetType = row.targetType,
                    weightKg = row.weightKg,
                    restSeconds = row.restSeconds,
                )
            }
        )
        id
    }

    /** Copies any plan (including a default one) into a new, editable user plan. */
    suspend fun duplicatePlan(id: Long): Long = db.withTransaction {
        val plan = requireNotNull(planDao.getPlan(id)) { "Plan $id not found" }
        val newId = planDao.insertPlan(
            plan.copy(id = 0, name = "${plan.name} (copy)", isDefault = false, createdAt = System.currentTimeMillis())
        )
        planDao.insertRows(
            planDao.getRows(id).map { row ->
                PlanExercise(
                    planId = newId,
                    exerciseId = row.exerciseId,
                    orderIndex = row.orderIndex,
                    sets = row.sets,
                    target = row.target,
                    targetType = row.targetType,
                    weightKg = row.weightKg,
                    restSeconds = row.restSeconds,
                )
            }
        )
        newId
    }

    suspend fun deletePlan(id: Long) {
        val plan = planDao.getPlan(id) ?: return
        require(!plan.isDefault) { "Default plans can't be deleted" }
        planDao.deletePlan(plan)
    }

    private suspend fun exerciseIdFor(row: PlanRowInput): Long {
        val name = row.exerciseName.trim()
        exerciseDao.findByName(name)?.let { return it.id }
        return exerciseDao.insert(
            Exercise(
                name = name,
                muscleGroup = row.muscleGroup?.trim()?.ifBlank { null },
                instructions = row.instructions?.trim()?.ifBlank { null },
            )
        )
    }
}
