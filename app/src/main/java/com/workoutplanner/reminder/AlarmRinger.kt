package com.workoutplanner.reminder

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * Loops the phone's alarm tone on the alarm stream, with vibration, until [stop].
 * The alarm stream plays even when the ringer is on silent or vibrate, like a clock app.
 */
class AlarmRinger(private val context: Context) {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    val isRinging get() = player != null

    fun start() {
        if (player != null) return
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        // Some phones have no alarm tone set, so fall back to the ringtone, then the notification sound.
        player = listOf(RingtoneManager.TYPE_ALARM, RingtoneManager.TYPE_RINGTONE, RingtoneManager.TYPE_NOTIFICATION)
            .firstNotNullOfOrNull { type ->
                val uri = RingtoneManager.getActualDefaultRingtoneUri(context, type) ?: return@firstNotNullOfOrNull null
                runCatching {
                    MediaPlayer().apply {
                        setAudioAttributes(attributes)
                        setDataSource(context, uri)
                        isLooping = true
                        prepare()
                        start()
                    }
                }.getOrNull()
            }

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (vibrator?.hasVibrator() == true) {
            @Suppress("DEPRECATION") // the AudioAttributes overload lets vibration through on silent, like the sound
            vibrator?.vibrate(VibrationEffect.createWaveform(VIBRATION, 0), attributes)
        }
    }

    fun stop() {
        player?.run {
            runCatching { stop() }
            release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private companion object {
        /** Buzz 0.6 s, pause 0.4 s, repeated. */
        val VIBRATION = longArrayOf(0, 600, 400)
    }
}
