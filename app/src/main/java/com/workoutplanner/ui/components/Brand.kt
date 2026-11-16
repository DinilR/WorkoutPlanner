package com.workoutplanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.workoutplanner.ui.theme.LocalWorkoutColors
import com.workoutplanner.ui.theme.brandGradient
import com.workoutplanner.ui.theme.gradientBarColors

/**
 * The app's top bar: a Red Orange gradient with faint racing stripes and a
 * Yellow Punk underline. Used on every screen in place of a plain TopAppBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .background(brandGradient())
                .diagonalStripes(Color.White.copy(alpha = 0.08f), spacing = 26.dp, width = 9.dp)
        ) {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = actions,
                colors = gradientBarColors(),
            )
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(LocalWorkoutColors.current.done))
    }
}

/** Diagonal stripes drawn behind the content, like a sports-poster texture. */
fun Modifier.diagonalStripes(color: Color, spacing: Dp = 24.dp, width: Dp = 8.dp): Modifier =
    clipToBounds().drawBehind {
        val step = spacing.toPx()
        val stroke = width.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(
                color = color,
                start = Offset(x, size.height),
                end = Offset(x + size.height, 0f),
                strokeWidth = stroke,
            )
            x += step
        }
    }
