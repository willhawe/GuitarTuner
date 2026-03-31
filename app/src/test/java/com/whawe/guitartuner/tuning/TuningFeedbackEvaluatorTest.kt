package com.whawe.guitartuner.tuning

import org.junit.Assert.assertEquals
import org.junit.Test

class TuningFeedbackEvaluatorTest {
    @Test
    fun fromCents_mapsAccuracyBandsAtThresholds() {
        val cases = listOf(
            0.0 to AccuracyBand.PERFECT,
            5.0 to AccuracyBand.PERFECT,
            8.0 to AccuracyBand.VERY_GOOD,
            18.0 to AccuracyBand.GOOD,
            30.0 to AccuracyBand.ADJUST,
            40.0 to AccuracyBand.WAY_OFF
        )

        for ((cents, expectedBand) in cases) {
            val feedback = TuningFeedbackEvaluator.fromCents(cents)
            assertEquals(expectedBand, feedback.accuracyBand)
        }
    }

    @Test
    fun fromCents_mapsNeedleOffsetAcrossVisibleRange() {
        val cases = listOf(
            -75.0 to -1.0f,
            -25.0 to -0.5f,
            0.0 to 0.0f,
            12.5 to 0.25f,
            60.0 to 1.0f
        )

        for ((cents, expectedOffset) in cases) {
            val feedback = TuningFeedbackEvaluator.fromCents(cents)
            assertEquals(expectedOffset, feedback.needleOffset, 0.0001f)
        }
    }
}
