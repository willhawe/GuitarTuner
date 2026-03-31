package com.whawe.guitartuner.tuning

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PitchDetectorTest {
    @Test
    fun detectPitch_identifiesReferenceDataset() {
        val cases = listOf(
            SignalCase(expectedNote = "E2", expectedFrequency = 82.41f, sampleRate = 44100, sampleCount = 4096),
            SignalCase(expectedNote = "A2", expectedFrequency = 110.00f, sampleRate = 48000, sampleCount = 4096),
            SignalCase(expectedNote = "A3", expectedFrequency = 220.00f, sampleRate = 44100, sampleCount = 4096),
            SignalCase(expectedNote = "A4", expectedFrequency = 440.00f, sampleRate = 44100, sampleCount = 4096),
            SignalCase(expectedNote = "E5", expectedFrequency = 659.25f, sampleRate = 48000, sampleCount = 4096)
        )

        for (case in cases) {
            val signal = sineWave(
                frequency = case.expectedFrequency,
                sampleRate = case.sampleRate,
                sampleCount = case.sampleCount,
                amplitude = 12_000.0
            )

            val detected = PitchDetector.detectPitch(signal, signal.size, case.sampleRate)
            val noteMatch = NoteMapper.findClosestNote(detected)
            val centsOff = NoteMapper.calculateCentsOff(detected, case.expectedFrequency)

            assertEquals(case.expectedNote, noteMatch.name)
            assertTrue("Expected cents error <= 40 but was $centsOff", abs(centsOff) <= 40.0)
        }
    }

    @Test
    fun detectPitch_handlesHarmonicRichSignal() {
        val sampleRate = 44100
        val signal = harmonicWave(
            frequency = 196.00f,
            sampleRate = sampleRate,
            sampleCount = 4096
        )

        val detected = PitchDetector.detectPitch(signal, signal.size, sampleRate)
        val noteMatch = NoteMapper.findClosestNote(detected)
        val centsOff = NoteMapper.calculateCentsOff(detected, 196.00f)

        assertEquals("G3", noteMatch.name)
        assertTrue("Expected cents error <= 40 but was $centsOff", abs(centsOff) <= 40.0)
    }

    @Test
    fun detectPitch_tracksDetunedPluckedStringSignal() {
        val sampleRate = 44100
        val cases = listOf(
            82.41f * centsRatio(-9.0),
            110.00f * centsRatio(13.0),
            196.00f * centsRatio(17.0),
            329.63f * centsRatio(-14.0)
        )

        for (frequency in cases) {
            val signal = pluckedStringWave(
                frequency = frequency,
                sampleRate = sampleRate,
                sampleCount = 4096
            )

            val detected = PitchDetector.detectPitch(signal, signal.size, sampleRate)
            val centsOff = NoteMapper.calculateCentsOff(detected, frequency)

            assertTrue(
                "Expected cents error <= 12 but was $centsOff for $frequency Hz",
                abs(centsOff) <= 12.0
            )
        }
    }

    @Test
    fun detectPitch_returnsZeroForQuietSignal() {
        val sampleRate = 44100
        val signal = sineWave(
            frequency = 440.00f,
            sampleRate = sampleRate,
            sampleCount = 4096,
            amplitude = 20.0
        )

        val detected = PitchDetector.detectPitch(signal, signal.size, sampleRate)

        assertEquals(0.0, detected.toDouble(), 0.0)
    }

    @Test
    fun detectPitch_returnsZeroForShortBuffers() {
        val sampleRate = 44100
        val signal = sineWave(
            frequency = 440.00f,
            sampleRate = sampleRate,
            sampleCount = 512,
            amplitude = 12_000.0
        )

        val detected = PitchDetector.detectPitch(signal, signal.size, sampleRate)

        assertEquals(0.0, detected.toDouble(), 0.0)
    }

    private fun sineWave(
        frequency: Float,
        sampleRate: Int,
        sampleCount: Int,
        amplitude: Double
    ): ShortArray {
        return ShortArray(sampleCount) { sample ->
            val radians = 2 * PI * frequency * sample / sampleRate
            (sin(radians) * amplitude).toInt().toShort()
        }
    }

    private fun harmonicWave(
        frequency: Float,
        sampleRate: Int,
        sampleCount: Int
    ): ShortArray {
        return ShortArray(sampleCount) { sample ->
            val fundamental = sin(2 * PI * frequency * sample / sampleRate)
            val harmonic = 0.35 * sin(2 * PI * frequency * 2 * sample / sampleRate)
            ((fundamental + harmonic) * 10_000).toInt().toShort()
        }
    }

    private fun pluckedStringWave(
        frequency: Float,
        sampleRate: Int,
        sampleCount: Int
    ): ShortArray {
        return ShortArray(sampleCount) { sample ->
            val time = sample.toDouble() / sampleRate
            val envelope = exp(-4.0 * sample / sampleCount)
            val fundamental = sin(2 * PI * frequency * time)
            val second = 0.45 * sin(2 * PI * frequency * 2 * time + 0.18)
            val third = 0.18 * sin(2 * PI * frequency * 3 * time + 0.43)
            ((fundamental + second + third) * envelope * 11_000).toInt().toShort()
        }
    }

    private fun centsRatio(cents: Double): Float {
        return 2.0.pow(cents / 1200.0).toFloat()
    }

    private data class SignalCase(
        val expectedNote: String,
        val expectedFrequency: Float,
        val sampleRate: Int,
        val sampleCount: Int
    )
}
