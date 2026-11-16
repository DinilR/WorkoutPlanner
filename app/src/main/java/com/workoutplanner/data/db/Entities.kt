package com.workoutplanner.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class Level { BEGINNER, INTERMEDIATE, ADVANCED }

/** Whether a plan row is counted in repetitions or held for a number of seconds (e.g. Plank). */
enum class TargetType { REPS, TIME }

enum class Rating { AWESOME, GOOD, TOUGH }

/** The exercise library. Plans reference exercises by id. */
@Entity(tableName = "exercises", indices = [Index(value = ["name"], unique = true)])
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String? = null,
    /** Numbered how-to steps, one per line. */
    val instructions: String? = null,
    val isDefault: Boolean = false,
)

@Entity(tableName = "plans")
data class WorkoutPlan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val level: Level = Level.BEGINNER,
    /** Built-in plans can be duplicated but not edited or deleted. */
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

/** One row of the plan table: Exercise · Sets · Reps/Time · kg · Rest. */
@Entity(
    tableName = "plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(entity = Exercise::class, parentColumns = ["id"], childColumns = ["exerciseId"]),
    ],
    indices = [Index("planId"), Index("exerciseId")],
)
data class PlanExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val sets: Int,
    /** Repetitions, or seconds when [targetType] is TIME. */
    val target: Int,
    val targetType: TargetType = TargetType.REPS,
    /** Null means bodyweight. */
    val weightKg: Double? = null,
    val restSeconds: Int = 90,
)

/**
 * One workout from start to finish. [finishedAt] is null while it is in progress,
 * which is how the app finds a workout to resume.
 *
 * The plan name is copied in so history survives the plan being deleted.
 */
@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutPlan::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("planId")],
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long?,
    val planName: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val rating: Rating? = null,
)

/**
 * One set bubble on the workout screen. The plan row is copied in when the session
 * starts, so editing the plan later never changes past workouts.
 */
@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val exerciseOrder: Int,
    val setNumber: Int,
    val targetType: TargetType,
    val targetValue: Int,
    /** What the user actually did; null until the set is completed or edited. */
    val actualValue: Int? = null,
    val weightKg: Double? = null,
    val restSeconds: Int,
    /** Null means the set is not done yet. */
    val completedAt: Long? = null,
)
