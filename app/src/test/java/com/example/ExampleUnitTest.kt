package com.example

import com.example.engine.MusicTheory
import com.example.model.MusicianRegistry
import com.example.model.ScaleType
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testMusicTheoryPitchConversion() {
        val a4Midi = MusicTheory.frequencyToMidi(440f)
        assertEquals(69f, a4Midi, 0.05f)

        val noteName = MusicTheory.midiToNoteName(69)
        assertEquals("A4", noteName)

        val c4Freq = MusicTheory.midiToFrequency(60f)
        assertEquals(261.63f, c4Freq, 0.5f)
    }

    @Test
    fun testMusicianRegistryContainsThirteenMusicians() {
        val musicians = MusicianRegistry.ALL_MUSICIANS
        assertEquals(15, musicians.size)

        val evh = MusicianRegistry.getById("eddie_van_halen")
        assertNotNull(evh)
        assertEquals("Eddie Van Halen", evh.name)

        val brubeck = MusicianRegistry.getById("dave_brubeck")
        assertNotNull(brubeck)
        assertEquals("Dave Brubeck", brubeck.name)

        val diagRoot = MusicianRegistry.getById("diagnostic_root")
        assertNotNull(diagRoot)
        assertEquals("diagnostic_root", diagRoot.id)

        val diagRootThird = MusicianRegistry.getById("diagnostic_root_third")
        assertNotNull(diagRootThird)
        assertEquals("diagnostic_root_third", diagRootThird.id)
    }

    @Test
    fun testScaleNotesGeneration() {
        val dDorianNotes = MusicTheory.getScaleNotes(
            rootMidiPitchClass = 2, // D
            scaleType = ScaleType.DORIAN,
            octaveRange = 4..4
        )
        assertTrue(dDorianNotes.contains(62)) // D4
        assertTrue(dDorianNotes.contains(64)) // E4
        assertTrue(dDorianNotes.contains(65)) // F4
        assertTrue(dDorianNotes.contains(67)) // G4
        assertTrue(dDorianNotes.contains(69)) // A4
    }

    @Test
    fun testSoloMuteState() {
        val synth = com.example.audio.PolySynth()
        assertFalse(synth.isSoloMuted)
        synth.isSoloMuted = true
        assertTrue(synth.isSoloMuted)
        synth.isSoloMuted = false
        assertFalse(synth.isSoloMuted)
    }
}
