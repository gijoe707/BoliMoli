package com.openpaperlabs.bolimoli.core.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressLogicTest {

    @Test
    fun testStarsFor_scoringRules() {
        // 0 wrong answers -> 3 stars
        assertEquals(3, ProgressLogic.starsFor(wrongAnswers = 0, usedSelfCheck = false))
        // 1 or 2 wrong answers -> 2 stars
        assertEquals(2, ProgressLogic.starsFor(wrongAnswers = 1, usedSelfCheck = false))
        assertEquals(2, ProgressLogic.starsFor(wrongAnswers = 2, usedSelfCheck = false))
        // 3 or more wrong answers -> 1 star
        assertEquals(1, ProgressLogic.starsFor(wrongAnswers = 3, usedSelfCheck = false))
        // Self-check caps at 2 stars
        assertEquals(2, ProgressLogic.starsFor(wrongAnswers = 0, usedSelfCheck = true))
    }

    @Test
    fun testStreakCalculation() {
        val today = 1000L
        var state = ProgressState()

        // Initial state -> 0 streak
        assertEquals(0, ProgressLogic.displayStreak(state, today))

        // Complete lesson today -> streak 1
        state = ProgressLogic.recordLessonCompleted(state, "unit_a", stars = 3, usedSelfCheck = false, today = today)
        assertEquals(1, ProgressLogic.displayStreak(state, today))

        // Complete lesson next day (today + 1) -> streak 2
        val nextDay = today + 1
        state = ProgressLogic.recordLessonCompleted(state, "unit_aa", stars = 3, usedSelfCheck = false, today = nextDay)
        assertEquals(2, ProgressLogic.displayStreak(state, nextDay))

        // Miss 2 days -> streak resets to 0
        val laterDay = nextDay + 3
        assertEquals(0, ProgressLogic.displayStreak(state, laterDay))
    }

    @Test
    fun testRecordActivityCompleted_updatesStarsAndStreak() {
        val today = 1000L
        var state = ProgressState()

        state = ProgressLogic.recordActivityCompleted(state, "quiz_level_1", starsEarned = 2, today = today)
        assertEquals(2, state.totalStars)
        assertEquals(1, ProgressLogic.displayStreak(state, today))

        // Replaying same activity with better score updates best score
        state = ProgressLogic.recordActivityCompleted(state, "quiz_level_1", starsEarned = 3, today = today)
        assertEquals(3, state.totalStars)
    }

    @Test
    fun testAddStudySeconds_cappingAndMinutesToday() {
        val today = 1000L
        var state = ProgressState()

        // 120 seconds -> 2 minutes
        state = ProgressLogic.addStudySeconds(state, 120, today)
        assertEquals(2, ProgressLogic.minutesToday(state, today))

        // Exceeding MAX_LESSON_VISIT_SECONDS (600s = 10 mins) gets capped
        state = ProgressLogic.addStudySeconds(state, 1200, today)
        // 120 + 600 = 720 seconds -> 12 minutes
        assertEquals(12, ProgressLogic.minutesToday(state, today))
    }
}
