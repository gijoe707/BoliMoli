package com.openpaperlabs.bolimoli.feature.parent

import com.openpaperlabs.bolimoli.core.curriculum.CurriculumRepository

enum class MasteryStatus {
    MASTERED,      // Green (>= 2 stars or completed)
    NEEDS_PRACTICE,// Red (attempted with 1 star / struggled)
    NOT_STARTED    // Grey (unattempted)
}

data class LetterMasteryItem(
    val unitId: String,
    val letter: String,
    val transliteration: String,
    val category: String, // "Uyir", "Mei", "Uyirmei"
    val accuracyPercentage: Int,
    val attemptsCount: Int,
    val status: MasteryStatus
)

data class DailyUsageStat(
    val dayName: String,
    val minutesSpent: Int,
    val isToday: Boolean = false
)

/**
 * One entry per letter (all 247), built from completed units and earned stars.
 * Letters are marked NEEDS_PRACTICE only if attempted and struggled (1 star).
 * Unattempted letters are marked NOT_STARTED.
 */
fun buildMasteryGrid(
    completedUnitIds: Set<String>,
    starsByUnit: Map<String, Int> = emptyMap()
): List<LetterMasteryItem> =
    CurriculumRepository.ALL_UNITS.map { unit ->
        val stars = starsByUnit[unit.id]
        val isCompleted = completedUnitIds.contains(unit.id)
        val status = when {
            isCompleted || (stars != null && stars >= 2) -> MasteryStatus.MASTERED
            stars == 1 -> MasteryStatus.NEEDS_PRACTICE
            else -> MasteryStatus.NOT_STARTED
        }
        val accuracy = when (status) {
            MasteryStatus.MASTERED -> 95
            MasteryStatus.NEEDS_PRACTICE -> 40
            MasteryStatus.NOT_STARTED -> 0
        }
        val attempts = when (status) {
            MasteryStatus.MASTERED -> 3
            MasteryStatus.NEEDS_PRACTICE -> 2
            MasteryStatus.NOT_STARTED -> 0
        }
        LetterMasteryItem(
            unitId = unit.id,
            letter = unit.text,
            transliteration = unit.transliteration,
            category = unit.kind.name,
            accuracyPercentage = accuracy,
            attemptsCount = attempts,
            status = status
        )
    }
