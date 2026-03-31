package com.whawe.guitartuner.tuning

import kotlin.math.abs

enum class AccuracyBand {
    PERFECT,
    VERY_GOOD,
    GOOD,
    ADJUST,
    WAY_OFF
}

data class TuningFeedback(
    val accuracyBand: AccuracyBand,
    val needleOffset: Float
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

        val needleOffset = (cents / MAX_VISIBLE_CENTS).coerceIn(-1.0, 1.0).toFloat()

        return TuningFeedback(
            accuracyBand = accuracyBand,
            needleOffset = needleOffset
        )
    }

    private const val MAX_VISIBLE_CENTS = 50.0
}
