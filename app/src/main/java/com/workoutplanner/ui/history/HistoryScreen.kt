package com.workoutplanner.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.data.db.SessionSummary
import com.workoutplanner.ui.components.GradientTopBar
import com.workoutplanner.ui.emoji
import com.workoutplanner.ui.formatDateTime
import com.workoutplanner.ui.formatMinutes
import com.workoutplanner.ui.formatVolume
import com.workoutplanner.ui.theme.LocalWorkoutColors
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private const val HEATMAP_WEEKS = 12

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            GradientTopBar(
                title = { Text("HISTORY") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val current = state
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "stats") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("WORKOUTS", "${current.total}", Modifier.weight(1f))
                    StatCard("STREAK", "🔥 ${current.streak}", Modifier.weight(1f))
                    StatCard("THIS WEEK", "${current.thisWeek}", Modifier.weight(1f))
                }
            }
            item(key = "heatmap") { HeatmapCard(perDay = current.perDay, today = current.today) }
            item(key = "title") {
                Text(
                    "PAST WORKOUTS",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (current.sessions.isEmpty()) {
                item(key = "empty") {
                    Card(Modifier.fillMaxWidth()) {
                        Text(
                            "No workouts yet. Finish one with SAVE WORKOUT and it shows up here.",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            items(current.sessions, key = { it.sessionId }) { session ->
                SessionCard(session, onClick = { onOpenSession(session.sessionId) })
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * The last [HEATMAP_WEEKS] weeks: one column per week (oldest on the left), Monday at the
 * top. Red Orange is one workout that day, Yellow Punk is two or more; today is outlined.
 */
@Composable
private fun HeatmapCard(perDay: Map<LocalDate, Int>, today: LocalDate) {
    val empty = MaterialTheme.colorScheme.surfaceContainer
    val one = MaterialTheme.colorScheme.primary
    val more = LocalWorkoutColors.current.done
    val outline = MaterialTheme.colorScheme.onSurface
    val firstMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks((HEATMAP_WEEKS - 1).toLong())
    val trainedDays = perDay.keys.count { !it.isBefore(firstMonday) && !it.isAfter(today) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("LAST $HEATMAP_WEEKS WEEKS", style = MaterialTheme.typography.titleMedium)
            Text(
                "$trainedDays training ${if (trainedDays == 1) "day" else "days"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val gap = 4.dp
                val labelWidth = 18.dp
                // Square cells sized to fill the width, so the weekday labels line up exactly.
                val cell = (maxWidth - labelWidth - gap * HEATMAP_WEEKS) / HEATMAP_WEEKS
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    Column(Modifier.width(labelWidth), verticalArrangement = Arrangement.spacedBy(gap)) {
                        listOf("M", "", "W", "", "F", "", "S").forEach { label ->
                            Box(Modifier.height(cell), contentAlignment = Alignment.CenterStart) {
                                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    repeat(HEATMAP_WEEKS) { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                            repeat(7) { dayIndex ->
                                val day = firstMonday.plusWeeks(week.toLong()).plusDays(dayIndex.toLong())
                                val count = perDay[day] ?: 0
                                val color = when {
                                    day.isAfter(today) -> Color.Transparent
                                    count >= 2 -> more
                                    count == 1 -> one
                                    else -> empty
                                }
                                Box(
                                    Modifier
                                        .size(cell)
                                        .clip(CutCornerShape(topStart = 3.dp, bottomEnd = 3.dp))
                                        .background(color)
                                        .then(if (day == today) Modifier.border(2.dp, outline, CutCornerShape(topStart = 3.dp, bottomEnd = 3.dp)) else Modifier)
                                        .semantics { contentDescription = "$day: $count workouts" }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LegendSwatch(empty); Text("Rest", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(6.dp))
                LegendSwatch(one); Text("1 workout", style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.width(6.dp))
                LegendSwatch(more); Text("2+", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(Modifier.size(12.dp).clip(CutCornerShape(topStart = 2.dp, bottomEnd = 2.dp)).background(color))
}

@Composable
private fun SessionCard(session: SessionSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    formatDateTime(session.startedAt).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(session.planName.uppercase(), style = MaterialTheme.typography.titleLarge)
                Text(
                    buildString {
                        append(formatMinutes(session.finishedAt - session.startedAt))
                        append(" · ${session.setsDone}/${session.totalSets} sets")
                        if (session.volumeKg > 0) append(" · ${formatVolume(session.volumeKg)}")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            session.rating?.let {
                Text(it.emoji, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            }
        }
    }
}
