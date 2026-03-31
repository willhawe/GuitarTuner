package com.whawe.guitartuner.tuning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrequencySmootherTest {
    @Test
    fun add_tracksGradualPitchChangesWithoutSticking() {
        val smoother = FrequencySmoother()

        val first = smoother.add(100f)
        val second = smoother.add(101f)
        val third = smoother.add(102f)
        val fourth = smoother.add(103f)

        assertEquals(100f, first, 0.0001f)
        assertTrue(second > first)
        assertTrue(third > second)
        assertTrue(fourth > third)
        assertTrue(second < 101f)
        assertTrue(third < 102f)
        assertTrue(fourth < 103f)
    }

    @Test
    fun add_dampensSmallJitterWithoutFreezingTheEstimate() {
        val smoother = FrequencySmoother()

        smoother.add(440f)
        val smoothed = smoother.add(441f)

        assertTrue(smoothed > 440f)
        assertTrue(smoothed < 440.3f)
    }

    @Test
    fun add_reactsQuicklyToLargePitchChanges() {
        val smoother = FrequencySmoother()

        smoother.add(110f)
        val shifted = smoother.add(146.83f)

        assertTrue(shifted > 130f)
        assertTrue(shifted < 146.83f)
    }

    @Test
    fun clear_resetsInternalState() {
        val smoother = FrequencySmoother()

        smoother.add(100f)
        smoother.add(110f)
        smoother.clear()
        val reading = smoother.add(220f)

        assertEquals(220f, reading, 0.0001f)
    }
}
