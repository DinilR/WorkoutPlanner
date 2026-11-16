package com.workoutplanner.ui.finish

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class Piece(
    val x: Float,
    val delay: Float,
    val speed: Float,
    val drift: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
)

private val ConfettiColors = listOf(
    Color(0xFF1E9E55), Color(0xFF0E7C66), Color(0xFFE0752D),
    Color(0xFFFFC94A), Color(0xFF3D7BD9), Color(0xFFE04F7A),
)

/** A one-off burst of falling confetti, about three seconds long. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 90) {
    val pieces = remember {
        val random = Random(System.nanoTime())
        List(count) {
            Piece(
                x = random.nextFloat(),
                delay = random.nextFloat() * 0.35f,
                speed = 0.8f + random.nextFloat() * 0.7f,
                drift = (random.nextFloat() - 0.5f) * 0.25f,
                spin = (random.nextFloat() - 0.5f) * 1440f,
                size = 8f + random.nextFloat() * 10f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(3200, easing = LinearEasing)) }

    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        pieces.forEach { p ->
            val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val x = (p.x + p.drift * local) * size.width
            val y = -40f + local * p.speed * (size.height + 80f)
            val alpha = if (local > 0.8f) (1f - local) / 0.2f else 1f
            rotate(degrees = p.spin * local, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2, y - p.size / 4),
                    size = Size(p.size, p.size / 2),
                )
            }
        }
    }
}
