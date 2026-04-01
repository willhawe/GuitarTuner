package com.whawe.guitartuner.scale

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScaleLibraryTest {
    @Test
    fun buildScale_returnsExpectedNotesForCMajor() {
        val scale = ScaleLibrary.buildScale(RootNote.C, ScaleType.MAJOR)

        assertEquals(
            listOf("C4", "D4", "E4", "F4", "G4", "A4", "B4", "C5"),
            scale.notes.map { it.name }
        )
    }

    @Test
    fun buildScale_returnsExpectedNotesForAMinorPentatonic() {
        val scale = ScaleLibrary.buildScale(RootNote.A, ScaleType.MINOR_PENTATONIC)

        assertEquals(
            listOf("A4", "C5", "D5", "E5", "G5", "A5"),
            scale.notes.map { it.name }
        )
    }

    @Test
    fun buildScale_returnsExpectedNotesForCHarmonicMinor() {
        val scale = ScaleLibrary.buildScale(RootNote.C, ScaleType.HARMONIC_MINOR)

        assertEquals(
            listOf("C4", "D4", "D#4", "F4", "G4", "G#4", "B4", "C5"),
            scale.notes.map { it.name }
        )
    }

    @Test
    fun buildScale_returnsExpectedNotesForDMixolydian() {
        val scale = ScaleLibrary.buildScale(RootNote.D, ScaleType.MIXOLYDIAN)

        assertEquals(
            listOf("D4", "E4", "F#4", "G4", "A4", "B4", "C5", "D5"),
            scale.notes.map { it.name }
        )
    }

    @Test
    fun buildScale_returnsExpectedNotesForEMinorBlues() {
        val scale = ScaleLibrary.buildScale(RootNote.E, ScaleType.MINOR_BLUES)

        assertEquals(
            listOf("E4", "G4", "A4", "A#4", "B4", "D5", "E5"),
            scale.notes.map { it.name }
        )
    }

    @Test
    fun containsPitchClass_matchesAcrossOctaves() {
        val scale = ScaleLibrary.buildScale(RootNote.E, ScaleType.MAJOR_BLUES)

        assertTrue(scale.containsPitchClass(4))
        assertTrue(scale.containsPitchClass(16))
        assertTrue(scale.containsPitchClass(-8))
        assertFalse(scale.containsPitchClass(5))
    }
}
