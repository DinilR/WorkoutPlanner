package com.workoutplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand palette: Red Orange, Snowy White and Yellow Punk.
// Red Orange leads (bars, main buttons), Yellow Punk fills cards and controls,
// and Snowy White is kept for text on Red Orange and the set bubbles.
private val RedOrange = Color(0xFFE8452C)
private val RedOrangeBright = Color(0xFFFF8A70)
private val Peach = Color(0xFFFFE3D8)
private val PeachDeep = Color(0xFFFFD2C2)
private val SnowyWhite = Color(0xFFFFFAFA)
private val YellowPunk = Color(0xFFFFD000)
private val YellowPunkSoft = Color(0xFFFFD23F)
private val YellowPale = Color(0xFFFFF0B3)
private val Ink = Color(0xFF231917)

private val LightColors = lightColorScheme(
    primary = RedOrange,
    onPrimary = SnowyWhite,
    primaryContainer = PeachDeep,
    onPrimaryContainer = Color(0xFF3D0700),
    secondary = Color(0xFF77574F),
    onSecondary = SnowyWhite,
    // Yellow Punk on the steppers and tonal buttons.
    secondaryContainer = YellowPunk,
    onSecondaryContainer = Color(0xFF2B2100),
    tertiary = Color(0xFFD9A400),
    onTertiary = Color(0xFF2B2100),
    tertiaryContainer = YellowPale,
    onTertiaryContainer = Color(0xFF2B2100),
    // A Red Orange tint instead of a white page.
    background = Peach,
    onBackground = Ink,
    surface = Peach,
    onSurface = Ink,
    surfaceVariant = PeachDeep,
    onSurfaceVariant = Color(0xFF5A4038),
    surfaceContainerLowest = SnowyWhite,
    surfaceContainerLow = Color(0xFFFFEDE6),
    surfaceContainer = PeachDeep,
    // Cards: pale Yellow Punk on the peach background.
    surfaceContainerHigh = YellowPale,
    surfaceContainerHighest = YellowPale,
    outline = Color(0xFF8C665C),
    outlineVariant = Color(0xFFEBB3A2),
)

private val DarkColors = darkColorScheme(
    primary = RedOrangeBright,
    onPrimary = Color(0xFF5A1000),
    primaryContainer = Color(0xFF8C2A16),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFFE7BDB4),
    secondaryContainer = YellowPunkSoft,
    onSecondaryContainer = Color(0xFF2B2100),
    tertiary = YellowPunkSoft,
    onTertiary = Color(0xFF3A2E00),
    tertiaryContainer = Color(0xFF574500),
    background = Color(0xFF2A1510),
    onBackground = Color(0xFFF1DFDB),
    surface = Color(0xFF2A1510),
    onSurface = Color(0xFFF1DFDB),
    surfaceVariant = Color(0xFF4A2E26),
    onSurfaceVariant = Color(0xFFE0C4BC),
    surfaceContainerLowest = Color(0xFF1A0E0B),
    surfaceContainerLow = Color(0xFF331C16),
    surfaceContainer = Color(0xFF40231B),
    surfaceContainerHigh = Color(0xFF3D3200),
    surfaceContainerHighest = Color(0xFF3D3200),
    outline = Color(0xFFA88C84),
    outlineVariant = Color(0xFF5E4038),
)

/** Colours for workout feedback that Material's scheme doesn't cover. */
@Immutable
data class WorkoutColors(
    /** Fill of a completed set bubble and the finished progress ring. */
    val done: Color,
    val onDone: Color,
    /** A finished exercise card. */
    val doneContainer: Color,
    /** Readable text colour for "done" labels (yellow alone is too light on the background). */
    val doneText: Color,
    val record: Color,
)

private val LightWorkoutColors = WorkoutColors(
    done = YellowPunk,
    onDone = Color(0xFF2B2100),
    doneContainer = YellowPunk,
    doneText = Color(0xFF7A5C00),
    record = RedOrange,
)

private val DarkWorkoutColors = WorkoutColors(
    done = YellowPunkSoft,
    onDone = Color(0xFF2B2100),
    doneContainer = YellowPunkSoft,
    doneText = YellowPunkSoft,
    record = RedOrangeBright,
)

val LocalWorkoutColors = staticCompositionLocalOf { LightWorkoutColors }

/** The signature Red Orange → orange sweep used on top bars and hero blocks. */
@Composable
fun brandGradient(): Brush = Brush.linearGradient(
    if (isSystemInDarkTheme()) listOf(Color(0xFFB8321C), Color(0xFFC95A12))
    else listOf(RedOrange, Color(0xFFFF7A1A))
)

/** Transparent bar with Snowy White content, drawn on top of [brandGradient]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun gradientBarColors(): TopAppBarColors = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.Transparent,
    titleContentColor = SnowyWhite,
    navigationIconContentColor = SnowyWhite,
    actionIconContentColor = SnowyWhite,
)

@Composable
fun WorkoutPlannerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalWorkoutColors provides if (darkTheme) DarkWorkoutColors else LightWorkoutColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
