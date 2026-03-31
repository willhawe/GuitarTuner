package com.whawe.guitartuner.tuning

class FrequencySmoother(
    private val windowSize: Int = 5
) {
    private val history = ArrayDeque<Float>()

    init {
        require(windowSize > 0) { "windowSize must be positive" }
    }

    fun add(frequency: Float): Float {
        history.addLast(frequency)
        while (history.size > windowSize) {
            history.removeFirst()
        }

        val sorted = history.sorted()
        val medianIndex = sorted.size / 2

        return if (sorted.size % 2 == 0) {
            (sorted[medianIndex - 1] + sorted[medianIndex]) / 2
        } else {
            sorted[medianIndex]
        }
    }

    fun clear() {
        history.clear()
    }
}
