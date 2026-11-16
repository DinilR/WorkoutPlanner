package com.workoutplanner.data.seed

import androidx.room.withTransaction
import com.workoutplanner.data.db.AppDatabase
import com.workoutplanner.data.db.Exercise
import com.workoutplanner.data.db.PlanExercise
import com.workoutplanner.data.db.WorkoutPlan

object DatabaseSeeder {

    /**
     * Inserts any [DefaultPlans] exercises and plans that aren't in the database yet,
     * matched by name. Runs on every launch, so plans added in an app update appear
     * on existing installs without touching the user's own plans or history.
     */
    suspend fun seed(db: AppDatabase) = db.withTransaction {
        val exerciseDao = db.exerciseDao()
        val planDao = db.planDao()

        val exerciseIds = DefaultPlans.exercises.associate { seed ->
            val id = exerciseDao.findByName(seed.name)?.id ?: exerciseDao.insert(
                Exercise(
                    name = seed.name,
                    muscleGroup = seed.muscleGroup,
                    instructions = seed.steps.joinToString("\n"),
                    isDefault = true,
                )
            )
            seed.name to id
        }

        for (seedPlan in DefaultPlans.plans) {
            if (planDao.findDefaultPlan(seedPlan.name) != null) continue
            val planId = planDao.insertPlan(
                WorkoutPlan(
                    name = seedPlan.name,
                    description = seedPlan.description,
                    level = seedPlan.level,
                    isDefault = true,
                )
            )
            planDao.insertRows(
                seedPlan.rows.mapIndexed { index, row ->
                    PlanExercise(
                        planId = planId,
                        exerciseId = exerciseIds.getValue(row.exercise),
                        orderIndex = index,
                        sets = row.sets,
                        target = row.target,
                        targetType = row.targetType,
                        weightKg = row.weightKg,
                        restSeconds = row.rest,
                    )
                }
            )
        }
    }
}
