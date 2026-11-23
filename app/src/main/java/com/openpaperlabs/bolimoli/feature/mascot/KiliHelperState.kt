package com.openpaperlabs.bolimoli.feature.mascot

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.openpaperlabs.bolimoli.core.audio.AppAudioEngine
import com.openpaperlabs.bolimoli.core.mascot.KiliCoach
import com.openpaperlabs.bolimoli.core.mascot.KiliStep
import com.openpaperlabs.bolimoli.core.mascot.KiliTapCounter
import com.openpaperlabs.bolimoli.core.speech.WordMatching

/** What Kili's speech bubble shows: the [parts] being said, with [activePart] lit up (-1 = all said). */
data class KiliSpeech(val caption: String, val parts: List<String>, val activePart: Int)

/**
 * Everything Kili the parrot does on a screen. She is quiet until she is touched ([help]) or until the child has
 * finished something ([cheer]).
 *
 *  - [help] says one [KiliStep] part by part, lighting up each part in the bubble and moving her beak. Touching her
 *    again for the same thing moves on to the next step of the plan (letters, then syllables, then the whole word).
 *  - While she speaks, [onSpeakingStart] is called once and, when she is done (or interrupted), [onSpeakingEnd] once,
 *    so a screen with an open microphone can close it for that moment and open it again.
 *
 * Call everything on the main thread.
 */
class KiliHelperState(private val audio: AppAudioEngine) {

    var mood: KiliMood by mutableStateOf(KiliMood.IDLE)
        private set

    var speech: KiliSpeech? by mutableStateOf(null)
        private set

    /** A short line instead of parts, e.g. the praise shown while she cheers. */
    var note: String? by mutableStateOf(null)
        private set

    var onSpeakingStart: (() -> Unit)? = null
    var onSpeakingEnd: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val taps = KiliTapCounter()
    private var session = 0
    private var holding = false
    private var released = false

    private val clearTask = Runnable {
        speech = null
        note = null
    }

    private val idleTask = Runnable {
        mood = KiliMood.IDLE
        note = null
    }

    /**
     * The child touched Kili for [key] (the word or letter being practised). Says the next step of [plan]:
     * the first touch for a key says step 1, the second step 2, and so on; touches past the end repeat the last step.
     */
    fun help(key: String, plan: List<KiliStep>) {
        if (released || plan.isEmpty()) return
        say(KiliCoach.stepFor(plan, taps.next(key)))
    }

    /** Kili jumps for joy for a moment, with [text] in her bubble. She says nothing aloud. */
    fun cheer(text: String? = null) {
        if (released) return
        session++ // whatever she was saying is cut short
        handler.removeCallbacks(clearTask)
        handler.removeCallbacks(idleTask)
        speech = null
        note = text
        mood = KiliMood.CHEER
        endHold()
        handler.postDelayed(idleTask, CHEER_MS)
    }

    /** Back to the start: quiet, and the next touch begins with the first step again. */
    fun reset() {
        session++
        handler.removeCallbacksAndMessages(null)
        taps.reset()
        speech = null
        note = null
        mood = KiliMood.IDLE
        endHold()
    }

    fun release() {
        released = true
        session++
        handler.removeCallbacksAndMessages(null)
        holding = false
    }

    // ---- speaking ---------------------------------------------------------------------------------

    private fun say(step: KiliStep) {
        handler.removeCallbacks(clearTask)
        handler.removeCallbacks(idleTask)
        val mine = ++session
        note = null
        mood = KiliMood.TALKING
        if (!holding) {
            holding = true
            onSpeakingStart?.invoke()
        }
        sayPart(mine, step, 0)
    }

    private fun sayPart(mine: Int, step: KiliStep, index: Int) {
        if (released || mine != session) return
        if (index >= step.parts.size) {
            finish(mine, step)
            return
        }
        speech = KiliSpeech(step.caption, step.parts, index)
        val part = step.parts[index]
        val spoken = WordMatching.key(part).ifEmpty { part }
        audio.speakTamilSequenceThen(listOf(spoken), 0L) { completed ->
            if (released || mine != session) return@speakTamilSequenceThen
            if (!completed) {
                finish(mine, step)
                return@speakTamilSequenceThen
            }
            if (step.parts.size > 1) {
                handler.postDelayed({ sayPart(mine, step, index + 1) }, PART_PAUSE_MS)
            } else {
                sayPart(mine, step, index + 1)
            }
        }
    }

    private fun finish(mine: Int, step: KiliStep) {
        if (mine != session) return
        mood = KiliMood.IDLE
        speech = KiliSpeech(step.caption, step.parts, -1)
        endHold()
        handler.postDelayed(clearTask, BUBBLE_LINGER_MS)
    }

    private fun endHold() {
        if (holding) {
            holding = false
            onSpeakingEnd?.invoke()
        }
    }

    companion object {
        /** Silence between two spoken parts of the same step. */
        const val PART_PAUSE_MS = 350L

        /** The bubble stays this long after she has finished speaking. */
        const val BUBBLE_LINGER_MS = 3500L

        /** How long she keeps cheering. */
        const val CHEER_MS = 2800L
    }
}
