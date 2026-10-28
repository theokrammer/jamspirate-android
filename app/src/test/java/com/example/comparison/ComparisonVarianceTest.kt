package com.example.comparison

import com.example.ai.CloudLlmClient
import com.example.ai.LlmProvider
import com.example.audio.BackingPlayer
import com.example.audio.PolySynth
import com.example.engine.Improviser
import com.example.model.MusicianRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ComparisonVarianceTest {

    @Test
    fun testBackingTrackMultiBarProgressions() {
        val app = RuntimeEnvironment.getApplication()
        val player = BackingPlayer(app) { _, _, _ -> }

        // Test Take Five progression
        val takeFiveTrack = player.builtInTracks.first { it.id == "take_five" }
        val bar0 = player.getChordContextForBar(takeFiveTrack, 0)
        val bar1 = player.getChordContextForBar(takeFiveTrack, 1)
        assertEquals("Ebm7", bar0.chordName)
        assertEquals("Bbm7", bar1.chordName)
        assertNotEquals(bar0.rootPitchClass, bar1.rootPitchClass)

        // Test Chicago Blues progression
        val bluesTrack = player.builtInTracks.first { it.id == "chicago_blues" }
        val b0 = player.getChordContextForBar(bluesTrack, 0)
        val b4 = player.getChordContextForBar(bluesTrack, 4)
        val b8 = player.getChordContextForBar(bluesTrack, 8)
        assertEquals("A7", b0.chordName)
        assertEquals("D7", b4.chordName)
        assertEquals("E7", b8.chordName)
    }

    @Test
    fun testPromptIncludesFullBackingTrackContextAndDiversityRules() {
        val client = CloudLlmClient()
        val musician = MusicianRegistry.getById("eric_johnson")
        val app = RuntimeEnvironment.getApplication()
        val player = BackingPlayer(app) { _, _, _ -> }
        val track = player.builtInTracks.first { it.id == "take_five" }

        val chords = (0 until 4).map { b -> Pair(b, player.getChordContextForBar(track, b)) }
        val prompt = client.buildPrompt(
            musician = musician,
            bpm = track.bpm,
            key = track.defaultKey,
            upcomingBars = chords,
            customDirective = "Cascading pentatonic triplets with hybrid picking",
            useFamousPieceMode = false,
            timeSignature = track.timeSignature,
            beatsPerBar = track.beatsPerBar,
            trackTitle = track.title,
            trackStyle = track.artistOrStyle,
            variantLabel = "Variant A: Cascades"
        )

        // Assert full backing track information is included
        assertTrue(prompt.contains("Take Five"))
        assertTrue(prompt.contains("Dave Brubeck Style"))
        assertTrue(prompt.contains("5/4"))
        assertTrue(prompt.contains("Measure 1: Chord Ebm7"))
        assertTrue(prompt.contains("Measure 2: Chord Bbm7"))

        // Assert diversity and multi-measure arc rules are present
        assertTrue(prompt.contains("MULTI-MEASURE ARC"))
        assertTrue(prompt.contains("RHYTHMIC VARIANCE"))
    }

    @Test
    fun testComparisonVariantsProduceDistinctRhythmicAndMelodicProfiles() = runBlocking {
        val synth = PolySynth()
        val improviser = Improviser(synth) {}
        val client = CloudLlmClient()
        val engine = ComparisonBatchEngine(client, improviser)

        val app = RuntimeEnvironment.getApplication()
        val player = BackingPlayer(app) { _, _, _ -> }
        val track = player.builtInTracks.first { it.id == "take_five" }

        val chords = (0 until 4).map { b -> Pair(b, player.getChordContextForBar(track, b)) }

        val variants = listOf(
            ComparisonVariant(
                id = "v1",
                label = "Cascading Triplets",
                musicianId = "eric_johnson",
                customDirective = "Rapid cascading pentatonic triplets"
            ),
            ComparisonVariant(
                id = "v2",
                label = "Lyrical Sustained",
                musicianId = "david_gilmour",
                customDirective = "Spacious lyrical sustained singing notes"
            ),
            ComparisonVariant(
                id = "v3",
                label = "Outside Chromatic",
                musicianId = "john_coltrane",
                customDirective = "Outside chromatic passing tones and substitutions"
            ),
            ComparisonVariant(
                id = "v4",
                label = "Take Five Desmond Motif",
                musicianId = "paul_desmond",
                useFamousPieceMode = true,
                famousPieceName = "Take Five"
            )
        )

        val session = ComparisonSession(
            trackId = track.id,
            trackTitle = track.title,
            trackStyle = track.artistOrStyle,
            bpm = track.bpm,
            key = track.defaultKey,
            timeSignature = track.timeSignature,
            beatsPerBar = track.beatsPerBar,
            barCount = 4,
            chords = chords,
            variants = variants
        )

        val results = engine.runComparisonBatch(session, LlmProvider.LOCAL_THEORY, apiKey = "") {}

        assertEquals(4, results.size)

        val v1Notes = results[0].notes
        val v2Notes = results[1].notes
        val v3Notes = results[2].notes
        val v4Notes = results[3].notes

        // 1. Note count variance: Cascades should have much higher density than Lyrical
        println("Note Counts: V1(Cascades)=${v1Notes.size}, V2(Lyrical)=${v2Notes.size}, V3(Chromatic)=${v3Notes.size}, V4(Desmond)=${v4Notes.size}")
        assertTrue("Cascades (${v1Notes.size}) should have more notes than lyrical (${v2Notes.size})", v1Notes.size > v2Notes.size)

        // 2. Rhythmic variance across measures within the same variant
        val v1Bar0 = v1Notes.filter { it.barIndex == 0 }.map { it.subInBar }
        val v1Bar1 = v1Notes.filter { it.barIndex == 1 }.map { it.subInBar }
        assertNotEquals("Bar 0 and Bar 1 should have different rhythms", v1Bar0, v1Bar1)

        val v4Bar0 = v4Notes.filter { it.barIndex == 0 }.map { it.subInBar }
        val v4Bar3 = v4Notes.filter { it.barIndex == 3 }.map { it.subInBar }
        assertNotEquals("Desmond Bar 0 (exposition) and Bar 3 (resolution) should have different rhythms", v4Bar0, v4Bar3)

        // 3. Pitch variance between variants
        val v1Pitches = v1Notes.map { it.midiPitch }.toSet()
        val v2Pitches = v2Notes.map { it.midiPitch }.toSet()
        assertNotEquals("V1 and V2 should have different pitch choices", v1Pitches, v2Pitches)
    }
}
