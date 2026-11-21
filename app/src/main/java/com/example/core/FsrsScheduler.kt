package com.example.core

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * FSRS-inspired spaced repetition scheduler weighting mastery, lapses,
 * pronunciation score, and response speed.
 */
object FsrsScheduler {

    data class ScheduleResult(
        val masteryScore: Int,
        val stabilityDays: Double,
        val difficulty: Double,
        val reviewCount: Int,
        val lapseCount: Int,
        val nextReviewEpochMs: Long
    )

    /**
     * @param rating 1=Again, 2=Hard, 3=Good, 4=Easy
     */
    fun computeNext(
        currentMastery: Int,
        currentStability: Double,
        currentDifficulty: Double,
        reviewCount: Int,
        lapseCount: Int,
        pronunciationScore: Int,
        responseTimeMs: Long,
        rating: Int,
        nowMs: Long = System.currentTimeMillis()
    ): ScheduleResult {
        val speedBonus = when {
            responseTimeMs <= 3500L -> 1.1
            responseTimeMs <= 8000L -> 1.0
            else -> 0.9
        }
        val pronFactor = if (pronunciationScore > 0) (0.75 + (pronunciationScore / 400.0)) else 1.0

        val newDifficulty = when (rating) {
            1 -> min(10.0, currentDifficulty + 1.2)
            2 -> min(10.0, currentDifficulty + 0.45)
            3 -> max(1.0, currentDifficulty - 0.15)
            else -> max(1.0, currentDifficulty - 0.55)
        }

        val newStability = when (rating) {
            1 -> max(0.25, currentStability * 0.35)
            2 -> max(0.5, currentStability * 1.25 * pronFactor)
            3 -> max(1.0, currentStability * (2.1 - (newDifficulty * 0.08)) * speedBonus * pronFactor)
            else -> max(2.0, currentStability * (2.9 - (newDifficulty * 0.08)) * speedBonus * pronFactor)
        }.coerceIn(0.25, 3650.0)

        val deltaMastery = when (rating) {
            1 -> -20
            2 -> 8
            3 -> 16
            else -> 24
        }
        val newMastery = (currentMastery + deltaMastery).coerceIn(5, 100)
        val newLapses = if (rating == 1) lapseCount + 1 else lapseCount
        val intervalMs = (newStability * 24.0 * 60.0 * 60.0 * 1000.0).toLong()

        return ScheduleResult(
            masteryScore = newMastery,
            stabilityDays = (newStability * 100.0).roundToInt() / 100.0,
            difficulty = (newDifficulty * 100.0).roundToInt() / 100.0,
            reviewCount = reviewCount + 1,
            lapseCount = newLapses,
            nextReviewEpochMs = nowMs + intervalMs
        )
    }
}
