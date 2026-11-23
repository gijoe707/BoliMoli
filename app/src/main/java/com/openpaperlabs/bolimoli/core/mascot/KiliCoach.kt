package com.openpaperlabs.bolimoli.core.mascot

import com.openpaperlabs.bolimoli.core.text.TamilSyllableSegmenter
import com.openpaperlabs.bolimoli.core.text.TamilTextSegmenter

/** How Kili says something: letter by letter, in syllables, or the whole word. */
enum class KiliStage { LETTERS, SYLLABLES, WORD }

/**
 * One thing Kili can say when touched: [parts] are spoken one after the other and shown in her speech bubble
 * under [caption].
 */
data class KiliStep(val stage: KiliStage, val parts: List<String>, val caption: String)

/**
 * Counts how often Kili has been touched for the same thing. Touching her for something new (another word, another
 * letter) starts again from the first step. Pure Kotlin, so it can be unit tested.
 */
class KiliTapCounter {
    private var key: String? = null
    private var count = 0

    /** Returns 0 for the first touch on [newKey], 1 for the second, and so on. */
    fun next(newKey: String): Int {
        if (newKey != key) {
            key = newKey
            count = 0
        }
        return count++
    }

    fun reset() {
        key = null
        count = 0
    }
}

/**
 * Decides what Kili says. The first touch spells a word letter by letter, the second says it syllable by syllable,
 * the third (and every touch after it) says the whole word. Steps that would repeat the previous one are left
 * out (a one-syllable word has no syllable step). Pure Kotlin, no Android classes.
 */
object KiliCoach {

    const val CAPTION_SPELL = "Spell it"
    const val CAPTION_PARTS = "In parts"
    const val CAPTION_WORD = "The word"
    const val CAPTION_WORD_BY_WORD = "Word by word"
    const val CAPTION_TOGETHER = "Together"
    const val CAPTION_SOUND = "The sound"
    const val CAPTION_EXAMPLE = "Example word"

    /** Steps for a word or a short phrase. Empty when [word] is blank. */
    fun planForWord(word: String): List<KiliStep> {
        val clean = word.trim()
        if (clean.isEmpty()) return emptyList()

        // A phrase: say it word by word, then all together
        val pieces = clean.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (pieces.size > 1) {
            return listOf(
                KiliStep(KiliStage.SYLLABLES, pieces, CAPTION_WORD_BY_WORD),
                KiliStep(KiliStage.WORD, listOf(clean), CAPTION_TOGETHER)
            )
        }

        val letters = TamilTextSegmenter.segmentGraphemes(clean).map { it.text }.filter { it.isNotBlank() }
        val syllables = TamilSyllableSegmenter.segmentSyllables(clean).filter { it.isNotBlank() }

        val steps = ArrayList<KiliStep>()
        fun add(stage: KiliStage, parts: List<String>, caption: String) {
            if (parts.isEmpty()) return
            if (steps.isNotEmpty() && steps.last().parts == parts) return
            // One part that is the whole word is simply "the word"
            val whole = parts.size == 1 && parts[0] == clean
            steps += KiliStep(
                stage = if (whole) KiliStage.WORD else stage,
                parts = parts,
                caption = if (whole) CAPTION_WORD else caption
            )
        }
        add(KiliStage.LETTERS, letters, CAPTION_SPELL)
        add(KiliStage.SYLLABLES, syllables, CAPTION_PARTS)
        add(KiliStage.WORD, listOf(clean), CAPTION_WORD)
        return steps
    }

    /**
     * Steps for a letter test: first the sound of the letter itself, then an example word that contains it
     * (in parts, then whole).
     */
    fun planForLetter(letter: String, keyword: String): List<KiliStep> {
        val steps = ArrayList<KiliStep>()
        val l = letter.trim()
        if (l.isNotEmpty()) steps += KiliStep(KiliStage.LETTERS, listOf(l), CAPTION_SOUND)
        val k = keyword.trim()
        if (k.isNotEmpty()) {
            steps += planForWord(k)
                .filter { it.stage != KiliStage.LETTERS }
                .map { it.copy(caption = CAPTION_EXAMPLE) }
        }
        return steps
    }

    /** The step for the [tapIndex]-th touch (0 = first); touches past the end repeat the last step. */
    fun stepFor(plan: List<KiliStep>, tapIndex: Int): KiliStep = plan[tapIndex.coerceIn(0, plan.lastIndex)]
}
