package com.workoutplanner.ui.theme

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Cut corners on opposite diagonals instead of soft rounded ones. Cards, dialogs,
 * chips and the FAB all pick these up from the theme.
 */
val AppShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
    medium = CutCornerShape(topStart = 18.dp, bottomEnd = 18.dp),
    large = CutCornerShape(topStart = 22.dp, bottomEnd = 22.dp),
    extraLarge = CutCornerShape(topStart = 28.dp, bottomEnd = 28.dp),
)

/** Material buttons are always pill-shaped, so the main buttons pass this explicitly. */
val PunkButtonShape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp)
