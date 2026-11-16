package com.workoutplanner.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.workoutplanner.R

/** Anton: a heavy, condensed poster face for headings and numbers (SIL OFL, see /licenses). */
val Anton = FontFamily(Font(R.font.anton_regular, FontWeight.Normal))

private val base = Typography()

/** Anton for display, headline and title text; the system font for body copy. */
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Anton),
    displayMedium = base.displayMedium.copy(fontFamily = Anton),
    displaySmall = base.displaySmall.copy(fontFamily = Anton),
    headlineLarge = base.headlineLarge.copy(fontFamily = Anton),
    headlineMedium = base.headlineMedium.copy(fontFamily = Anton, letterSpacing = 0.5.sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = Anton, letterSpacing = 0.5.sp),
    titleLarge = base.titleLarge.copy(fontFamily = Anton, fontSize = 24.sp, letterSpacing = 1.sp),
    titleMedium = base.titleMedium.copy(fontFamily = Anton, fontSize = 19.sp, letterSpacing = 0.6.sp),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp),
)
