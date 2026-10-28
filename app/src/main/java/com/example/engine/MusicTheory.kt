package com.example.engine

import com.example.model.ScaleType
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt

object MusicTheory {
    private val NOTE_NAMES = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    fun frequencyToMidi(freq: Float): Float {
        if (freq <= 0f) return 0f
        return 69f + 12f * (ln(freq / 440.0) / ln(2.0)).toFloat()
    }

    fun midiToFrequency(midi: Float): Float {
        return (440.0 * 2.0.pow((midi.toDouble() - 69.0) / 12.0)).toFloat()
    }

    fun midiToNoteName(midi: Int): String {
        if (midi < 0 || midi > 127) return "--"
        val noteIndex = midi % 12
        val octave = (midi / 12) - 1
        return "${NOTE_NAMES[noteIndex]}$octave"
    }

    fun rootNameToMidiRoot(name: String): Int {
        val clean = name.trim().uppercase()
        return when {
            clean.startsWith("C#") || clean.startsWith("DB") -> 1
            clean.startsWith("D#") || clean.startsWith("EB") -> 3
            clean.startsWith("F#") || clean.startsWith("GB") -> 6
            clean.startsWith("G#") || clean.startsWith("AB") -> 8
            clean.startsWith("A#") || clean.startsWith("BB") -> 10
            clean.startsWith("C") -> 0
            clean.startsWith("D") -> 2
            clean.startsWith("E") -> 4
            clean.startsWith("F") -> 5
            clean.startsWith("G") -> 7
            clean.startsWith("A") -> 9
            clean.startsWith("B") -> 11
            else -> 0 // default C
        }
    }

    fun getScaleNotes(rootMidiPitchClass: Int, scaleType: ScaleType, octaveRange: IntRange = 3..6): List<Int> {
        val notes = mutableListOf<Int>()
        for (oct in octaveRange) {
            val base = (oct + 1) * 12 + (rootMidiPitchClass % 12)
            for (interval in scaleType.intervals) {
                val midi = base + interval
                if (midi in 36..96) {
                    notes.add(midi)
                }
            }
        }
        return notes.sorted()
    }

    fun snapToScale(
        rawMidi: Float,
        rootPitchClass: Int,
        scaleType: ScaleType,
        allowOutside: Boolean = false,
        outsideProb: Float = 0.1f
    ): Int {
        val scaleNotes = getScaleNotes(rootPitchClass, scaleType)
        if (scaleNotes.isEmpty()) return rawMidi.roundToInt()

        if (allowOutside && Math.random().toFloat() < outsideProb) {
            // Pick a chromatic neighbor of the nearest note
            val nearest = scaleNotes.minByOrNull { kotlin.math.abs(it - rawMidi) } ?: 60
            return (nearest + if (Math.random() > 0.5) 1 else -1).coerceIn(36, 96)
        }

        return scaleNotes.minByOrNull { kotlin.math.abs(it - rawMidi) } ?: rawMidi.roundToInt()
    }

    fun getCentsOffset(freq: Float, targetMidi: Int): Float {
        if (freq <= 0f) return 0f
        val exactMidi = frequencyToMidi(freq)
        return (exactMidi - targetMidi) * 100f
    }
}
