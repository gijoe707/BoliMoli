package com.openpaperlabs.bolimoli.core.mascot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KiliCoachTest {

    @Test
    fun tapCounter_countsPerKeyAndRestartsForANewKey() {
        val taps = KiliTapCounter()
        assertEquals(0, taps.next("அம்மா"))
        assertEquals(1, taps.next("அம்மா"))
        assertEquals(2, taps.next("அம்மா"))
        assertEquals(0, taps.next("பூனை"))
        assertEquals(1, taps.next("பூனை"))
        taps.reset()
        assertEquals(0, taps.next("பூனை"))
    }

    @Test
    fun word_firstSpellsThenSyllablesThenWholeWord() {
        val plan = KiliCoach.planForWord("அம்மா")
        assertEquals(listOf(KiliStage.LETTERS, KiliStage.SYLLABLES, KiliStage.WORD), plan.map { it.stage })
        assertEquals(listOf("அ", "ம்", "மா"), plan[0].parts)
        assertEquals(listOf("அம்", "மா"), plan[1].parts)
        assertEquals(listOf("அம்மா"), plan[2].parts)
    }

    @Test
    fun word_stepThatRepeatsThePreviousOneIsLeftOut() {
        // three letters, and each one is a syllable of its own: no separate syllable step
        val plan = KiliCoach.planForWord("பலகை")
        assertEquals(listOf(KiliStage.LETTERS, KiliStage.WORD), plan.map { it.stage })
    }

    @Test
    fun word_singleLetterWordIsJustTheWord() {
        val plan = KiliCoach.planForWord("கை")
        assertEquals(1, plan.size)
        assertEquals(KiliStage.WORD, plan[0].stage)
        assertEquals(listOf("கை"), plan[0].parts)
    }

    @Test
    fun phrase_isSaidWordByWordThenTogether() {
        val plan = KiliCoach.planForWord("மீண்டும் சந்திப்போம்")
        assertEquals(2, plan.size)
        assertEquals(listOf("மீண்டும்", "சந்திப்போம்"), plan[0].parts)
        assertEquals(listOf("மீண்டும் சந்திப்போம்"), plan[1].parts)
    }

    @Test
    fun blankWord_hasNoPlan() {
        assertTrue(KiliCoach.planForWord("   ").isEmpty())
    }

    @Test
    fun touchesPastTheEnd_repeatTheWholeWord() {
        val plan = KiliCoach.planForWord("அம்மா")
        assertEquals(KiliStage.LETTERS, KiliCoach.stepFor(plan, 0).stage)
        assertEquals(KiliStage.SYLLABLES, KiliCoach.stepFor(plan, 1).stage)
        assertEquals(KiliStage.WORD, KiliCoach.stepFor(plan, 2).stage)
        assertEquals(KiliStage.WORD, KiliCoach.stepFor(plan, 3).stage)
        assertEquals(KiliStage.WORD, KiliCoach.stepFor(plan, 40).stage)
    }

    @Test
    fun letter_firstTheSoundThenAnExampleWord() {
        val plan = KiliCoach.planForLetter("அ", "அம்மா")
        assertEquals(listOf("அ"), plan[0].parts)
        assertEquals(KiliCoach.CAPTION_SOUND, plan[0].caption)
        assertTrue(plan.drop(1).all { it.caption == KiliCoach.CAPTION_EXAMPLE })
        assertEquals(listOf("அம்மா"), plan.last().parts)
    }

    @Test
    fun letter_withoutAnExampleWord_isJustTheSound() {
        val plan = KiliCoach.planForLetter("அ", "")
        assertEquals(1, plan.size)
    }
}
