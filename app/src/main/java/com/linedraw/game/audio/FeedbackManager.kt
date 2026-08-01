package com.linedraw.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import com.linedraw.game.R

/**
 * Central sound + haptic feedback. Both channels are individually
 * toggleable from Settings; callers just fire semantic events.
 */
class FeedbackManager(context: Context) {

    @Volatile var soundEnabled: Boolean = true
    @Volatile var hapticsEnabled: Boolean = true

    private val vibrator = context.getSystemService(Vibrator::class.java)

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val connectSound = soundPool.load(context, R.raw.connect, 1)
    private val completeSound = soundPool.load(context, R.raw.complete, 1)
    private val stuckSound = soundPool.load(context, R.raw.stuck, 1)

    /** Soft chime + light tick each time a dot is connected; pitch rises along the path. */
    fun dotConnected(pathLength: Int) {
        if (soundEnabled) {
            val rate = (1f + (pathLength % 10) * 0.04f).coerceIn(0.5f, 2f)
            soundPool.play(connectSound, 0.6f, 0.6f, 1, 0, rate)
        }
        if (hapticsEnabled) {
            vibrator?.vibrate(VibrationEffect.createOneShot(12, 80))
        }
    }

    /** Warm two-note chime + medium pulse on level complete. */
    fun levelComplete() {
        if (soundEnabled) {
            soundPool.play(completeSound, 0.8f, 0.8f, 1, 0, 1f)
        }
        if (hapticsEnabled) {
            vibrator?.vibrate(VibrationEffect.createOneShot(40, 160))
        }
    }

    /** Low soft tone + gentle double tap on dead end — "pause", not "fail". */
    fun stuck() {
        if (soundEnabled) {
            soundPool.play(stuckSound, 0.5f, 0.5f, 1, 0, 1f)
        }
        if (hapticsEnabled) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 20, 80, 20), intArrayOf(0, 60, 0, 60), -1),
            )
        }
    }
}
