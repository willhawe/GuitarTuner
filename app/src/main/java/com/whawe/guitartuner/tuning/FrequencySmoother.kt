package com.whawe.guitartuner.tuning

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln

class FrequencySmoother(
    // 0.25 gives ~43% of a correction within 3 frames vs 37% at 0.18, making fine-tuning
    // feel noticeably more responsive while still heavily damping per-frame jitter.
    private val stableAlpha: Float = 0.25f,
    private val responsiveAlpha: Float = 0.35f,
    private val fastAlpha: Float = 0.60f,
    private val stableThresholdCents: Float = 10f,
    private val fastThresholdCents: Float = 35f
) {
    private var smoothedLogFrequency: Double? = null

    init {
        require(stableAlpha in 0f..1f) { "stableAlpha must be between 0 and 1" }
        require(responsiveAlpha in 0f..1f) { "responsiveAlpha must be between 0 and 1" }
        require(fastAlpha in 0f..1f) { "fastAlpha must be between 0 and 1" }
        require(stableThresholdCents > 0f) { "stableThresholdCents must be positive" }
        require(fastThresholdCents > stableThresholdCents) {
            "fastThresholdCents must be greater than stableThresholdCents"
        }
    }

    fun add(frequency: Float): Float {
        if (frequency <= 0f) {
            return 0f
        }

        val logFrequency = ln(frequency.toDouble())
        val previous = smoothedLogFrequency
        if (previous == null) {
            smoothedLogFrequency = logFrequency
            return frequency
        }

        val centsDelta = abs((logFrequency - previous) * CENTS_PER_OCTAVE / LN_2)
        val alpha = when {
            centsDelta <= stableThresholdCents -> stableAlpha.toDouble()
            centsDelta >= fastThresholdCents -> fastAlpha.toDouble()
            else -> responsiveAlpha.toDouble()
        }

        val smoothed = previous + (logFrequency - previous) * alpha
        smoothedLogFrequency = smoothed
        return exp(smoothed).toFloat()
    }

    fun clear() {
        smoothedLogFrequency = null
    }

    private companion object {
        const val CENTS_PER_OCTAVE = 1200.0
        val LN_2 = ln(2.0)
    }
}
