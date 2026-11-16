package com.workoutplanner.ui.workout

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Two short buzzes so the user feels that rest is over without looking at the phone. */
fun Context.vibrateRestOver() {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
    if (vibrator?.hasVibrator() == true) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 180, 120, 180), -1))
    }
}
