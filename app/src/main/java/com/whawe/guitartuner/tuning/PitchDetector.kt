package com.whawe.guitartuner.tuning

import kotlin.math.sqrt

object PitchDetector {
    fun detectPitch(buffer: ShortArray, size: Int, sampleRate: Int): Float {
        val actualSize = size.coerceAtMost(buffer.size)
        if (actualSize < 1024) {
            return 0f
        }

        val floatBuffer = FloatArray(actualSize)
        var mean = 0f
        for (index in 0 until actualSize) {
            mean += buffer[index]
        }
        mean /= actualSize

        var rms = 0f
        for (index in 0 until actualSize) {
            val centered = buffer[index] - mean
            floatBuffer[index] = centered
            rms += centered * centered
        }
        rms = sqrt(rms / actualSize)

        if (rms < 30f) {
            return 0f
        }

        val tauMax = actualSize / 2
        val difference = FloatArray(tauMax)
        for (tau in 1 until tauMax) {
            var sum = 0f
            var sampleIndex = 0
            val limit = actualSize - tau
            while (sampleIndex < limit) {
                val delta = floatBuffer[sampleIndex] - floatBuffer[sampleIndex + tau]
                sum += delta * delta
                sampleIndex++
            }
            difference[tau] = sum
        }

        val cumulativeMeanNormalizedDifference = FloatArray(tauMax)
        var runningSum = 0f
        cumulativeMeanNormalizedDifference[0] = 1f
        for (tau in 1 until tauMax) {
            runningSum += difference[tau]
            cumulativeMeanNormalizedDifference[tau] = if (runningSum == 0f) {
                1f
            } else {
                difference[tau] * tau / runningSum
            }
        }

        val threshold = 0.10f
        var tauEstimate = -1
        var minimumValue = Float.MAX_VALUE
        var minimumTau = -1

        var tau = 2
        while (tau < tauMax) {
            val currentValue = cumulativeMeanNormalizedDifference[tau]
            if (currentValue < threshold) {
                var localMinimumTau = tau
                var localMinimumValue = currentValue

                while (localMinimumTau + 1 < tauMax &&
                    cumulativeMeanNormalizedDifference[localMinimumTau + 1] <= localMinimumValue
                ) {
                    localMinimumTau++
                    localMinimumValue = cumulativeMeanNormalizedDifference[localMinimumTau]
                }

                tauEstimate = localMinimumTau
                break
            }

            if (currentValue < minimumValue) {
                minimumValue = currentValue
                minimumTau = tau
            }

            tau++
        }

        if (tauEstimate == -1 && minimumTau > 0 && minimumValue < 0.25f) {
            tauEstimate = minimumTau
        }

        if (tauEstimate <= 0) {
            return 0f
        }

        val previous = if (tauEstimate > 1) {
            cumulativeMeanNormalizedDifference[tauEstimate - 1]
        } else {
            cumulativeMeanNormalizedDifference[tauEstimate]
        }
        val next = if (tauEstimate + 1 < tauMax) {
            cumulativeMeanNormalizedDifference[tauEstimate + 1]
        } else {
            cumulativeMeanNormalizedDifference[tauEstimate]
        }
        val denominator = 2 * (2 * cumulativeMeanNormalizedDifference[tauEstimate] - previous - next)
        val refinedTau = if (denominator != 0f) {
            tauEstimate + (next - previous) / denominator
        } else {
            tauEstimate.toFloat()
        }

        val detectedFrequency = sampleRate / refinedTau
        return if (detectedFrequency in 15f..4000f) detectedFrequency else 0f
    }
}
