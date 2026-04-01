package com.whawe.guitartuner.scale

import com.whawe.guitartuner.tuning.NoteMapper

enum class RootNote(val displayName: String, val pitchClass: Int) {
    C("C", 0),
    C_SHARP("C#", 1),
    D("D", 2),
    D_SHARP("D#", 3),
    E("E", 4),
    F("F", 5),
    F_SHARP("F#", 6),
    G("G", 7),
    G_SHARP("G#", 8),
    A("A", 9),
    A_SHARP("A#", 10),
    B("B", 11);

    override fun toString(): String = displayName
}

enum class ScaleType(val displayName: String, val intervals: IntArray) {
    MAJOR("Major", intArrayOf(0, 2, 4, 5, 7, 9, 11, 12)),
    NATURAL_MINOR("Natural Minor", intArrayOf(0, 2, 3, 5, 7, 8, 10, 12)),
    MAJOR_PENTATONIC("Major Pentatonic", intArrayOf(0, 2, 4, 7, 9, 12)),
    MINOR_PENTATONIC("Minor Pentatonic", intArrayOf(0, 3, 5, 7, 10, 12)),
    MINOR_BLUES("Minor Blues", intArrayOf(0, 3, 5, 6, 7, 10, 12)),
    MAJOR_BLUES("Major Blues", intArrayOf(0, 2, 3, 4, 7, 9, 12)),
    DORIAN("Dorian", intArrayOf(0, 2, 3, 5, 7, 9, 10, 12)),
    MIXOLYDIAN("Mixolydian", intArrayOf(0, 2, 4, 5, 7, 9, 10, 12)),
    HARMONIC_MINOR("Harmonic Minor", intArrayOf(0, 2, 3, 5, 7, 8, 11, 12)),
    MELODIC_MINOR("Melodic Minor", intArrayOf(0, 2, 3, 5, 7, 9, 11, 12));

    override fun toString(): String = displayName
}

data class ScaleNote(
    val midiNumber: Int,
    val name: String,
    val pitchClass: Int
)

data class MusicalScale(
    val root: RootNote,
    val type: ScaleType,
    val notes: List<ScaleNote>
) {
    fun containsPitchClass(pitchClass: Int): Boolean {
        val normalized = ((pitchClass % 12) + 12) % 12
        return notes.any { it.pitchClass == normalized }
    }
}

object ScaleLibrary {
    fun buildScale(
        root: RootNote,
        type: ScaleType,
        startOctave: Int = 4
    ): MusicalScale {
        val rootMidiNumber = 12 * (startOctave + 1) + root.pitchClass
        val notes = type.intervals.map { interval ->
            val midiNumber = rootMidiNumber + interval
            ScaleNote(
                midiNumber = midiNumber,
                name = NoteMapper.noteNameForMidi(midiNumber),
                pitchClass = ((midiNumber % 12) + 12) % 12
            )
        }

        return MusicalScale(
            root = root,
            type = type,
            notes = notes
        )
    }
}
