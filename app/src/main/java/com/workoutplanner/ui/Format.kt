package com.workoutplanner.ui

import com.workoutplanner.data.db.Level
import com.workoutplanner.data.db.Rating
import com.workoutplanner.data.db.SetLog
import com.workoutplanner.data.db.TargetType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "12" for reps, "30s" for timed sets. */
fun formatTarget(value: Int, type: TargetType) = if (type == TargetType.TIME) "${value}s" else "$value"

/** "40 kg", "12.5 kg", or null for bodyweight. */
fun formatWeight(kg: Double?): String? = kg?.let {
    if (it % 1.0 == 0.0) "${it.toInt()} kg" else String.format(Locale.US, "%.1f kg", it)
}

fun formatRest(seconds: Int) = if (seconds >= 60 && seconds % 60 == 0) "${seconds / 60} min" else "${seconds}s"

/** "18:42" or "1:05:09". */
fun formatDuration(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
}

/** "32 min" or "1h 5m", for workout lengths in History (short enough for a stat tile). */
fun formatMinutes(millis: Long): String {
    val minutes = (millis / 60_000).coerceAtLeast(1)
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "$minutes min"
}

fun formatVolume(kg: Double): String = String.format(Locale.US, "%,d kg", kg.toLong())

private val dateTimeFormat = DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", Locale.getDefault())

/** "Fri 13 Nov · 5:42 PM" in the phone's time zone. */
fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormat)

val Rating.emoji get() = when (this) {
    Rating.AWESOME -> "😄"
    Rating.GOOD -> "🙂"
    Rating.TOUGH -> "😮‍💨"
}

/**
 * The "Last time" hint for an exercise: "Last time: 3×10 @ 40 kg" when every set was the
 * same, otherwise each set, e.g. "Last time: 10, 10, 8 @ 40 kg" or "10×40, 8×42.5 kg".
 */
fun formatLastTime(sets: List<SetLog>): String {
    val values = sets.map { formatTarget(it.actualValue ?: it.targetValue, it.targetType) }
    val weights = sets.map { it.weightKg }.distinct()
    val body = when {
        weights.size > 1 -> sets.joinToString(", ") { set ->
            val value = formatTarget(set.actualValue ?: set.targetValue, set.targetType)
            set.weightKg?.let { "$value×${formatWeight(it)!!.removeSuffix(" kg")}" } ?: value
        } + " kg"
        else -> {
            val reps = if (values.distinct().size == 1) "${sets.size}×${values.first()}" else values.joinToString(", ")
            reps + (formatWeight(weights.first())?.let { " @ $it" } ?: "")
        }
    }
    return "Last time: $body"
}

val Level.label get() = name.lowercase().replaceFirstChar { it.uppercase() }
