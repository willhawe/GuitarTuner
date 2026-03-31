package com.whawe.guitartuner.tuning

import org.junit.Assert.assertEquals
import org.junit.Test

class FrequencySmootherTest {
    @Test
    fun add_returnsMedianOfCurrentWindow() {
        val smoother = FrequencySmoother(windowSize = 5)

        smoother.add(440f)
        smoother.add(441f)
        val median = smoother.add(880f)

        assertEquals(441f, median, 0.0001f)
    }

    @Test
    fun add_discardsOldestReadingsWhenWindowIsFull() {
        val smoother = FrequencySmoother(windowSize = 3)

        smoother.add(100f)
        smoother.add(200f)
        smoother.add(300f)
        val median = smoother.add(400f)

        assertEquals(300f, median, 0.0001f)
    }

    @Test
    fun clear_resetsInternalState() {
        val smoother = FrequencySmoother(windowSize = 3)

        smoother.add(100f)
        smoother.add(110f)
        smoother.clear()
        val median = smoother.add(220f)

        assertEquals(220f, median, 0.0001f)
    }
}
