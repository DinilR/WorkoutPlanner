package com.workoutplanner.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/** The sounds a user can switch on or off, in the order they appear in Sound settings. */
enum class WorkoutSound(val label: String, val description: String) {
    SET_DONE("Set done", "Quick \"tr-ing\" when you tick a set"),
    EXERCISE_DONE("Exercise done", "Rising chime when every set of an exercise is done"),
    REST_COUNTDOWN("Rest countdown", "Clock tick every second of rest, beeps for the last 3"),
    REST_OVER("Rest over", "\"Ding-ding\" when rest ends"),
    WORKOUT_DONE("Workout done", "Fanfare when the last set of the workout is done"),
}

data class SoundSettings(
    /** The "All sounds" switch; when off nothing plays, whatever the per-sound switches say. */
    val master: Boolean = true,
    val enabled: Set<WorkoutSound> = WorkoutSound.entries.toSet(),
) {
    fun isOn(sound: WorkoutSound) = master && sound in enabled
}

/**
 * Plays workout sounds. The tones are synthesised in code (bell-like sine partials with a
 * fast attack and exponential decay), so there are no audio files and it works offline.
 *
 * Sounds use the media stream, so they follow the media volume and mix over music.
 * Settings are kept in SharedPreferences. Rendering and playback run off the main thread.
 */
class SoundPlayer(context: Context) {

    /** Every distinct sound the player can make. Rest countdown uses two of them. */
    private enum class Tone { SET, EXERCISE, TICK, BEEP, REST_OVER, FANFARE }

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val worker = Executors.newSingleThreadScheduledExecutor()
    private val cache = mutableMapOf<Tone, ShortArray>()

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<SoundSettings> = _settings.asStateFlow()

    fun setMaster(on: Boolean) {
        _settings.update { it.copy(master = on) }
        prefs.edit().putBoolean(KEY_MASTER, on).apply()
    }

    fun setEnabled(sound: WorkoutSound, on: Boolean) {
        _settings.update { it.copy(enabled = if (on) it.enabled + sound else it.enabled - sound) }
        prefs.edit().putBoolean(key(sound), on).apply()
    }

    /** Plays [sound] if the user has it switched on. */
    fun play(sound: WorkoutSound) {
        if (_settings.value.isOn(sound)) playTone(toneFor(sound))
    }

    /** One second of rest countdown: a clock tick, or a beep for the last 3 seconds. */
    fun restTick(secondsLeft: Int) {
        if (_settings.value.isOn(WorkoutSound.REST_COUNTDOWN)) {
            playTone(if (secondsLeft <= 3) Tone.BEEP else Tone.TICK)
        }
    }

    /** Plays [sound] even if it is switched off, for the ▶ buttons in Sound settings. */
    fun preview(sound: WorkoutSound) {
        if (sound == WorkoutSound.REST_COUNTDOWN) {
            // Two ticks then the three closing beeps, at real-time pace.
            listOf(Tone.TICK, Tone.TICK, Tone.BEEP, Tone.BEEP, Tone.BEEP).forEachIndexed { i, tone ->
                worker.schedule({ startTone(tone) }, i * 1000L, TimeUnit.MILLISECONDS)
            }
        } else {
            playTone(toneFor(sound))
        }
    }

    private fun toneFor(sound: WorkoutSound) = when (sound) {
        WorkoutSound.SET_DONE -> Tone.SET
        WorkoutSound.EXERCISE_DONE -> Tone.EXERCISE
        WorkoutSound.REST_COUNTDOWN -> Tone.TICK
        WorkoutSound.REST_OVER -> Tone.REST_OVER
        WorkoutSound.WORKOUT_DONE -> Tone.FANFARE
    }

    private fun playTone(tone: Tone) = worker.execute { startTone(tone) }

    /** Runs on [worker]. Tracks are released once they have finished, so sounds can overlap. */
    private fun startTone(tone: Tone) {
        val samples = cache.getOrPut(tone) { render(tone) }
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            worker.schedule({ track.release() }, samples.size * 1000L / SAMPLE_RATE + 100, TimeUnit.MILLISECONDS)
        }
    }

    private fun loadSettings() = SoundSettings(
        master = prefs.getBoolean(KEY_MASTER, true),
        enabled = WorkoutSound.entries.filter { prefs.getBoolean(key(it), true) }.toSet(),
    )

    private fun key(sound: WorkoutSound) = "sound_${sound.name.lowercase()}"

    /** One struck note. [partials] are (frequency multiple, relative loudness) pairs. */
    private data class Note(
        val freq: Double,
        val start: Double,
        val length: Double,
        val gain: Double = 1.0,
        val partials: List<Pair<Double, Double>> = BELL,
    )

    private fun render(tone: Tone): ShortArray = when (tone) {
        // Grace note flicking up into a bright E6: "tr-ing".
        Tone.SET -> mix(
            Note(B5, start = 0.0, length = 0.08, gain = 0.45),
            Note(E6, start = 0.045, length = 0.38),
        )
        // C6 then G6, both ringing: a step up.
        Tone.EXERCISE -> mix(
            Note(C6, start = 0.0, length = 0.5),
            Note(G6, start = 0.13, length = 0.7),
        )
        // A tiny woodblock knock, like a clock's second hand. Kept quiet so it doesn't nag.
        Tone.TICK -> mix(Note(TICK, start = 0.0, length = 0.035, partials = WOOD), peak = 0.35)
        // Plain short beep: the classic "3, 2, 1".
        Tone.BEEP -> mix(Note(A5 * 1.5, start = 0.0, length = 0.14, partials = PURE), peak = 0.7)
        // Two identical, woodier A5 hits: attention, go again.
        Tone.REST_OVER -> mix(
            Note(A5, start = 0.0, length = 0.28, partials = MARIMBA),
            Note(A5, start = 0.3, length = 0.4, partials = MARIMBA),
        )
        // C5 E5 G5 arpeggio landing on a C6 + E6 chord.
        Tone.FANFARE -> mix(
            Note(C5, start = 0.0, length = 0.35),
            Note(E5, start = 0.13, length = 0.35),
            Note(G5, start = 0.26, length = 0.35),
            Note(C6, start = 0.39, length = 1.2),
            Note(E6, start = 0.39, length = 1.2, gain = 0.5),
        )
    }

    /** Mixes [notes] into 16-bit mono PCM, normalised so the loudest point is [peak] of full scale. */
    private fun mix(vararg notes: Note, peak: Double = 0.85): ShortArray {
        val total = notes.maxOf { it.start + it.length }
        val buffer = DoubleArray((total * SAMPLE_RATE).toInt() + 1)
        for (note in notes) {
            val offset = (note.start * SAMPLE_RATE).toInt()
            val count = (note.length * SAMPLE_RATE).toInt()
            // Decays by about 45 dB over the note's length.
            val decay = 5.2 / note.length
            for (i in 0 until count) {
                val t = i.toDouble() / SAMPLE_RATE
                val attack = (t / ATTACK_SECONDS).coerceAtMost(1.0)
                val envelope = attack * exp(-decay * t) * note.gain
                var sample = 0.0
                for ((multiple, loudness) in note.partials) {
                    sample += loudness * sin(2 * PI * note.freq * multiple * t)
                }
                buffer[offset + i] += sample * envelope
            }
        }
        val loudest = buffer.maxOf { abs(it) }.takeIf { it > 0 } ?: 1.0
        val scale = peak * Short.MAX_VALUE / loudest
        return ShortArray(buffer.size) { (buffer[it] * scale).toInt().toShort() }
    }

    private companion object {
        const val KEY_MASTER = "sounds_enabled"
        const val SAMPLE_RATE = 44_100
        const val ATTACK_SECONDS = 0.002

        // Harmonic partials plus a slightly sharp one for shimmer: a glassy bell.
        val BELL = listOf(1.0 to 1.0, 2.0 to 0.35, 3.0 to 0.12, 4.2 to 0.06)

        // Strong fundamental with a 4th-harmonic knock: a rounder, woodier hit.
        val MARIMBA = listOf(1.0 to 1.0, 4.0 to 0.25, 10.0 to 0.04)

        // Inharmonic partials of a small block of wood.
        val WOOD = listOf(1.0 to 1.0, 2.57 to 0.5, 4.1 to 0.25)

        val PURE = listOf(1.0 to 1.0)

        const val C5 = 523.25
        const val E5 = 659.25
        const val G5 = 783.99
        const val A5 = 880.0
        const val B5 = 987.77
        const val C6 = 1046.5
        const val E6 = 1318.51
        const val G6 = 1567.98
        const val TICK = 1900.0
    }
}
