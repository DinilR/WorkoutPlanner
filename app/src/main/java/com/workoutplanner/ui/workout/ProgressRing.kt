package com.workoutplanner.ui.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.workoutplanner.ui.theme.LocalWorkoutColors

/** Big ring that fills as sets are completed, turning green when the workout is done. */
@Composable
fun ProgressRing(done: Int, total: Int, modifier: Modifier = Modifier) {
    val fraction = if (total == 0) 0f else done.toFloat() / total
    val animated by animateFloatAsState(fraction, tween(500), label = "ring")
    val color by animateColorAsState(
        if (total > 0 && done == total) LocalWorkoutColors.current.done else MaterialTheme.colorScheme.primary,
        label = "ringColor",
    )
    val track = MaterialTheme.colorScheme.surfaceContainerHigh

    Box(
        modifier.size(148.dp).semantics { contentDescription = "$done of $total sets done" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            drawArc(color, -90f, 360f * animated, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$done / $total", style = MaterialTheme.typography.displaySmall)
            Text("sets", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
