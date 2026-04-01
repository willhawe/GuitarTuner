package com.whawe.guitartuner.tuning

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

data class NoteMatch(
    val name: String,
    val targetFrequency: Float
)

object NoteMapper {
    private const val ConcertA = 440.0
    private val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun findClosestNote(frequency: Float): NoteMatch {
        if (frequency <= 0f) {
            return NoteMatch(name = "--", targetFrequency = 0f)
        }

        val midiNote = closestMidiNote(frequency)
        val targetFrequency = 440f * exp((ln(2.0) * (midiNote - 69) / 12.0)).toFloat()

        return NoteMatch(
            name = noteNameForMidi(midiNote),
            targetFrequency = targetFrequency
        )
    }

    fun closestMidiNote(frequency: Float): Int {
        return (12 * (ln(frequency / ConcertA) / ln(2.0))).roundToInt() + 69
    }

    fun noteNameForMidi(midiNote: Int): String {
        val noteIndex = normalizePitchClass(midiNote)
        val octave = midiNote / 12 - 1
        return "${noteNames[noteIndex]}$octave"
    }

    fun noteNameForPitchClass(pitchClass: Int): String {
        return noteNames[normalizePitchClass(pitchClass)]
    }

    fun pitchClassForNoteName(noteName: String): Int? {
        val letterPart = noteName.takeWhile { it.isLetter() || it == '#' }
        if (letterPart.isEmpty()) {
            return null
        }

        val index = noteNames.indexOf(letterPart)
        return if (index >= 0) index else null
    }

    fun calculateCentsOff(frequency: Float, targetFrequency: Float): Double {
        if (frequency <= 0f || targetFrequency <= 0f) {
            return 0.0
        }

        return 1200 * ln(frequency / targetFrequency) / ln(2.0)
    }

    private fun normalizePitchClass(value: Int): Int {
        return ((value % 12) + 12) % 12
    }
}
