package com.example.audio

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SoundEffects {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            Log.w("SoundEffects", "Could not initialize ToneGenerator", e)
        }
    }

    /**
     * Cheerful two-tone chime played when completing a step.
     */
    fun playStepCompleted(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                delay(140)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            } catch (e: Exception) {
                Log.w("SoundEffects", "Error playing chime", e)
            }
        }
    }

    /**
     * Celebratory flourish when routine finishes and enters bonus time.
     */
    fun playCelebration(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 100)
                delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                delay(120)
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 350)
            } catch (e: Exception) {
                Log.w("SoundEffects", "Error playing celebration", e)
            }
        }
    }

    /**
     * Gentle chime when timer is low.
     */
    fun playWarningTick(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            try {
                toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
            } catch (e: Exception) {
                Log.w("SoundEffects", "Error playing warning", e)
            }
        }
    }

    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            Log.w("SoundEffects", "Error releasing tone generator", e)
        }
    }
}
