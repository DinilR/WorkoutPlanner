package com.workoutplanner.ui.finish

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutplanner.data.db.Rating
import com.workoutplanner.ui.components.GradientTopBar
import com.workoutplanner.ui.theme.PunkButtonShape
import com.workoutplanner.ui.formatDuration
import com.workoutplanner.ui.formatVolume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishScreen(
    viewModel: FinishViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val streak by viewModel.streak.collectAsStateWithLifecycle()
    val rating by viewModel.rating.collectAsStateWithLifecycle()
    // Duration is frozen at the moment this screen opens.
    val endedAt = remember { System.currentTimeMillis() }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = { Text("DONE & DUSTED") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to workout")
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = summary != null,
                shape = PunkButtonShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .height(56.dp),
            ) { Text("SAVE WORKOUT", style = MaterialTheme.typography.titleMedium) }
        },
    ) { padding ->
        val s = summary
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (s == null) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(if (s.allDone) "🏆" else "💪", style = MaterialTheme.typography.displayLarge)
                Text(
                    if (s.allDone) "WORKOUT COMPLETE!" else "NICE WORK!",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
                Text(s.planName.uppercase(), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Time", formatDuration(endedAt - s.startedAt), Modifier.weight(1f))
                    StatCard("Sets", "${s.setsDone}/${s.totalSets}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Volume", formatVolume(s.volumeKg), Modifier.weight(1f))
                    StatCard(
                        "Streak",
                        streak?.let { "🔥 $it ${if (it == 1) "day" else "days"}" } ?: "…",
                        Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(28.dp))
                Text("How did it feel?", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    RatingCard("😄", "Awesome", rating == Rating.AWESOME, Modifier.weight(1f)) { viewModel.setRating(Rating.AWESOME) }
                    RatingCard("🙂", "Good", rating == Rating.GOOD, Modifier.weight(1f)) { viewModel.setRating(Rating.GOOD) }
                    RatingCard("😮‍💨", "Tough", rating == Rating.TOUGH, Modifier.weight(1f)) { viewModel.setRating(Rating.TOUGH) }
                }
                Spacer(Modifier.height(24.dp))
            }
            Confetti(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RatingCard(emoji: String, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
