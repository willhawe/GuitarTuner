package com.whawe.guitartuner.tuning

import kotlin.math.PI
import kotlin.math.abs
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
    private data class SignalCase(
        val expectedNote: String,
        val expectedFrequency: Float,
        val sampleRate: Int,
        val sampleCount: Int
    )
}
