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
    fun fromCents_mapsDialDirectionAndSeverity() {
        val cases = listOf(
            -30.0 to DialIndicator.LEFT_WARNING,
            -8.0 to DialIndicator.LEFT_CAUTION,
            0.0 to DialIndicator.CENTER_IN_TUNE,
            12.0 to DialIndicator.RIGHT_CAUTION,
            28.0 to DialIndicator.RIGHT_WARNING
        )

        for ((cents, expectedIndicator) in cases) {
            val feedback = TuningFeedbackEvaluator.fromCents(cents)
            assertEquals(expectedIndicator, feedback.dialIndicator)
        }
    }
}
