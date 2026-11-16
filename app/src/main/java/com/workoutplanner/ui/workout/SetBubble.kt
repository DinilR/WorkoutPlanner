package com.workoutplanner.ui.workout

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.workoutplanner.data.db.SetLog
import com.workoutplanner.data.db.TargetType
import com.workoutplanner.ui.formatDuration
import com.workoutplanner.ui.formatTarget
import com.workoutplanner.ui.formatWeight
import com.workoutplanner.ui.theme.LocalWorkoutColors

/**
 * One set. Tap to complete (fills from the centre and draws a tick), tap again to undo,
 * long-press to edit what you actually did.
 *
 * @param isNext the set the user should do next; it pulses gently.
 * @param restRemaining seconds left when this set is waiting on a rest countdown.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SetBubble(
    log: SetLog,
    isNext: Boolean,
    restRemaining: Int?,
    restFraction: Float,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalWorkoutColors.current
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val done = log.completedAt != null

    val fill by animateFloatAsState(if (done) 1f else 0f, tween(260, easing = FastOutSlowInEasing), label = "fill")
    val tick by animateFloatAsState(if (done) 1f else 0f, tween(300, delayMillis = if (done) 140 else 0), label = "tick")
    val pop by animateFloatAsState(if (done) 1f else 0.96f, spring(dampingRatio = 0.4f, stiffness = 600f), label = "pop")

    val pulsing = isNext && restRemaining == null && !done
    val pulse = if (pulsing) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "pulseScale",
        ).value
    } else 1f

    val value = log.actualValue ?: log.targetValue
    val unit = if (log.targetType == TargetType.TIME) "seconds" else "reps"
    val description = buildString {
        append("Set ${log.setNumber}, $value $unit")
        formatWeight(log.weightKg)?.let { append(" at $it") }
        append(if (done) ", done. Tap to undo." else ", not done. Tap to complete.")
    }

    val check = remember { Path() }
    val checkSegment = remember { Path() }
    val measure = remember { PathMeasure() }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(68.dp)
                .scale(pop * pulse)
                .clip(CircleShape)
                .combinedClickable(
                    role = Role.Checkbox,
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onTap()
                    },
                    onLongClick = onLongPress,
                    onLongClickLabel = "Edit reps and weight",
                )
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val radius = size.minDimension / 2 - stroke / 2
                // Base
                drawCircle(color = scheme.surfaceContainerLowest, radius = radius)
                drawCircle(
                    color = if (isNext && !done) scheme.primary else scheme.outlineVariant,
                    radius = radius,
                    style = Stroke(width = stroke),
                )
                // Fill grows from the centre; a dark rim keeps it visible on yellow cards.
                if (fill > 0f) {
                    drawCircle(color = colors.done, radius = (radius + stroke / 2) * fill)
                    drawCircle(color = colors.onDone.copy(alpha = fill), radius = radius, style = Stroke(width = stroke / 2))
                }
                // Rest countdown ring
                if (restRemaining != null) {
                    drawArc(
                        color = scheme.primary,
                        startAngle = -90f,
                        sweepAngle = 360f * restFraction,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
                // Tick draws itself in
                if (tick > 0f) {
                    check.reset()
                    check.moveTo(size.width * 0.29f, size.height * 0.52f)
                    check.lineTo(size.width * 0.44f, size.height * 0.67f)
                    check.lineTo(size.width * 0.72f, size.height * 0.37f)
                    measure.setPath(check, false)
                    checkSegment.reset()
                    measure.getSegment(0f, measure.length * tick, checkSegment, true)
                    drawPath(
                        checkSegment,
                        color = colors.onDone,
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
            if (!done) {
                Text(
                    text = restRemaining?.let { formatDuration(it * 1000L) } ?: formatTarget(log.targetValue, log.targetType),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (restRemaining != null) scheme.primary else scheme.onSurface,
                )
            }
        }
        Text(
            text = when {
                restRemaining != null -> "Rest"
                done -> buildString {
                    append(formatTarget(value, log.targetType))
                    log.weightKg?.let { append(" × ${formatWeight(it)!!.removeSuffix(" kg")}") }
                }
                else -> "Set ${log.setNumber}"
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (done) colors.doneText else scheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
