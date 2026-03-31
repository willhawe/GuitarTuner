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

        val midiNote = (12 * (ln(frequency / ConcertA) / ln(2.0))).roundToInt() + 69
        val noteIndex = ((midiNote % 12) + 12) % 12
        val octave = midiNote / 12 - 1
        val targetFrequency = 440f * exp((ln(2.0) * (midiNote - 69) / 12.0)).toFloat()

        return NoteMatch(
            name = "${noteNames[noteIndex]}$octave",
            targetFrequency = targetFrequency
        )
    }

    fun calculateCentsOff(frequency: Float, targetFrequency: Float): Double {
        if (frequency <= 0f || targetFrequency <= 0f) {
            return 0.0
        }

        return 1200 * ln(frequency / targetFrequency) / ln(2.0)
    }
}
