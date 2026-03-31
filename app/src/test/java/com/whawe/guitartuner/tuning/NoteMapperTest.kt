package com.whawe.guitartuner.tuning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMapperTest {
    @Test
    fun findClosestNote_mapsReferencePitchesAcrossOctaves() {
        val cases = listOf(
            82.41f to "E2",
            110.00f to "A2",
            146.83f to "D3",
            196.00f to "G3",
            246.94f to "B3",
            329.63f to "E4",
            440.00f to "A4"
        )

        for ((frequency, expectedNote) in cases) {
            val match = NoteMapper.findClosestNote(frequency)
            assertEquals(expectedNote, match.name)
        }
    }

    @Test
    fun findClosestNote_returnsPlaceholderForInvalidFrequency() {
        val match = NoteMapper.findClosestNote(0f)

        assertEquals("--", match.name)
        assertEquals(0f, match.targetFrequency, 0f)
    }

    @Test
    fun calculateCentsOff_returnsZeroForExactPitch() {
        val cents = NoteMapper.calculateCentsOff(440.0f, 440.0f)

        assertEquals(0.0, cents, 0.0001)
    }

    @Test
    fun calculateCentsOff_tracksSharpAndFlatReadings() {
        val sharp = NoteMapper.calculateCentsOff(445.0f, 440.0f)
        val flat = NoteMapper.calculateCentsOff(435.0f, 440.0f)

        assertTrue(sharp > 0.0)
        assertTrue(flat < 0.0)
    }
}
