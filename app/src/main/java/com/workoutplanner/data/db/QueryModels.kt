package com.workoutplanner.data.db

import androidx.room.Embedded

/** A plan card on the home screen. */
data class PlanSummary(
    @Embedded val plan: WorkoutPlan,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalRestSeconds: Int,
) {
    /** Rough duration: about 40 s of work per set plus the planned rest. */
    val estimatedMinutes: Int get() = ((totalSets * 40 + totalRestSeconds) / 60).coerceAtLeast(1)
}

/** A plan table row joined with its exercise. */
data class PlanRow(
    val id: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val sets: Int,
    val target: Int,
    val targetType: TargetType,
    val weightKg: Double?,
    val restSeconds: Int,
    val exerciseName: String,
    val muscleGroup: String?,
    val instructions: String?,
)

/** A set on the workout screen, with the exercise's how-to steps. */
data class SetLogWithInfo(
    @Embedded val log: SetLog,
    val instructions: String?,
)

/** One finished workout in the History list. */
data class SessionSummary(
    val sessionId: Long,
    val planName: String,
    val startedAt: Long,
    val finishedAt: Long,
    val rating: Rating?,
    val setsDone: Int,
    val totalSets: Int,
    val volumeKg: Double,
)

/** Drives the "Resume" banner on the home screen. */
data class ActiveSessionSummary(
    val sessionId: Long,
    val planName: String,
    val done: Int,
    val total: Int,
)
