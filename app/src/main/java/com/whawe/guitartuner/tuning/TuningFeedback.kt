package com.whawe.guitartuner.tuning

import kotlin.math.abs

enum class AccuracyBand {
    PERFECT,
    VERY_GOOD,
    GOOD,
    ADJUST,
    WAY_OFF
}

enum class DialIndicator {
    LEFT_WARNING,
    LEFT_CAUTION,
    CENTER_IN_TUNE,
    RIGHT_CAUTION,
    RIGHT_WARNING
}

data class TuningFeedback(
    val accuracyBand: AccuracyBand,
    val dialIndicator: DialIndicator
)

object TuningFeedbackEvaluator {
    fun fromCents(cents: Double): TuningFeedback {
        val absoluteCents = abs(cents)
        val accuracyBand = when {
            absoluteCents <= 5 -> AccuracyBand.PERFECT
            absoluteCents <= 10 -> AccuracyBand.VERY_GOOD
            absoluteCents <= 20 -> AccuracyBand.GOOD
            absoluteCents <= 35 -> AccuracyBand.ADJUST
            else -> AccuracyBand.WAY_OFF
        }

        val dialIndicator = when {
            cents < -20 -> DialIndicator.LEFT_WARNING
            cents > 20 -> DialIndicator.RIGHT_WARNING
            absoluteCents <= 5 -> DialIndicator.CENTER_IN_TUNE
            cents < 0 -> DialIndicator.LEFT_CAUTION
            else -> DialIndicator.RIGHT_CAUTION
        }

        return TuningFeedback(
            accuracyBand = accuracyBand,
            dialIndicator = dialIndicator
        )
    }
}
