package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import com.example.engine.MusicTheory
import com.example.model.BackingSourceType
import com.example.model.BackingTrack
import com.example.model.ScaleType
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class BackingPlayer(
    private val context: Context,
    private val onAudioFrameReady: (ShortArray, Int, Boolean) -> Unit // (buffer, length, isDownbeat)
) {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val FRAME_SIZE = 1024
    }

    private var mediaPlayer: MediaPlayer? = null
    private var backingAudioTrack: AudioTrack? = null
    private var backingRenderThread: Thread? = null
    private val isBackingRunning = AtomicBoolean(false)

    // Live Mic Looper
    private var audioRecord: AudioRecord? = null
    private var micRecordThread: Thread? = null
    private val isMicRecording = AtomicBoolean(false)
    private val recordedBuffer = mutableListOf<Short>()
    private var loopedAudioBuffer: ShortArray? = null
    private var loopPlayIndex = 0

    // Active playback state
    var onSequencerTick: ((barIndex: Int, subInBar: Int, beatInBar: Int, isBeat: Boolean, is8thNote: Boolean, chordContext: com.example.model.ChordContext) -> Unit)? = null

    var activeTrack: BackingTrack? = null
        private set
    var isPlaying: Boolean = false
        private set
    var volume: Float = 0.85f
        set(value) {
            field = value.coerceIn(0f, 1f)
            val effective = if (isMuted) 0f else field
            mediaPlayer?.setVolume(effective, effective)
        }
    var isMuted: Boolean = false
        set(value) {
            field = value
            val effective = if (value) 0f else volume
            mediaPlayer?.setVolume(effective, effective)
        }

    val builtInTracks = listOf(
        BackingTrack(
            id = "take_five",
            title = "Take Five (5/4 Swing)",
            artistOrStyle = "Dave Brubeck Style",
            bpm = 174,
            defaultKey = "Eb",
            defaultScale = ScaleType.DORIAN,
            recommendedMusicianId = "dave_brubeck",
            sourceType = BackingSourceType.PRESET,
            durationSec = 180,
            timeSignature = "5/4",
            beatsPerBar = 5
        ),
        BackingTrack(
            id = "invaders_rave",
            title = "Invaders Cyber-Rave",
            artistOrStyle = "The Prodigy Hook Style",
            bpm = 140,
            defaultKey = "F#",
            defaultScale = ScaleType.MINOR_PENTATONIC,
            recommendedMusicianId = "jimi_hendrix",
            sourceType = BackingSourceType.PRESET,
            durationSec = 150
        ),
        BackingTrack(
            id = "chicago_blues",
            title = "12-Bar Chicago Shuffle",
            artistOrStyle = "Slash & Hendrix Blues",
            bpm = 98,
            defaultKey = "A",
            defaultScale = ScaleType.BLUES,
            recommendedMusicianId = "slash",
            sourceType = BackingSourceType.PRESET,
            durationSec = 190
        ),
        BackingTrack(
            id = "kind_of_blue",
            title = "So What Modal Jazz",
            artistOrStyle = "Miles Davis Style",
            bpm = 112,
            defaultKey = "D",
            defaultScale = ScaleType.DORIAN,
            recommendedMusicianId = "miles_davis",
            sourceType = BackingSourceType.PRESET,
            durationSec = 210
        ),
        BackingTrack(
            id = "cliffs_cascade",
            title = "Melodic Cliffs Rock",
            artistOrStyle = "Eric Johnson Style",
            bpm = 128,
            defaultKey = "G",
            defaultScale = ScaleType.MAJOR_PENTATONIC,
            recommendedMusicianId = "eric_johnson",
            sourceType = BackingSourceType.PRESET,
            durationSec = 160
        ),
        BackingTrack(
            id = "flamenco_compas",
            title = "Bulerías Flamenco Compás",
            artistOrStyle = "Paco de Lucía Style",
            bpm = 180,
            defaultKey = "E",
            defaultScale = ScaleType.PHRYGIAN_DOMINANT,
            recommendedMusicianId = "paco_de_lucia",
            sourceType = BackingSourceType.PRESET,
            durationSec = 140
        ),
        BackingTrack(
            id = "sunset_metal",
            title = "Sunset Strip Heavy Metal",
            artistOrStyle = "Van Halen & Randy Rhoads",
            bpm = 138,
            defaultKey = "A",
            defaultScale = ScaleType.HARMONIC_MINOR,
            recommendedMusicianId = "eddie_van_halen",
            sourceType = BackingSourceType.PRESET,
            durationSec = 175
        ),
        BackingTrack(
            id = "pink_ballad",
            title = "Comfortably Slow Ballad",
            artistOrStyle = "David Gilmour Style",
            bpm = 74,
            defaultKey = "B",
            defaultScale = ScaleType.DORIAN,
            recommendedMusicianId = "david_gilmour",
            sourceType = BackingSourceType.PRESET,
            durationSec = 240
        ),
        BackingTrack(
            id = "baroque_prelude",
            title = "Brandenburg Basso Continuo",
            artistOrStyle = "J.S. Bach Style",
            bpm = 104,
            defaultKey = "D",
            defaultScale = ScaleType.MAJOR,
            recommendedMusicianId = "js_bach",
            sourceType = BackingSourceType.PRESET,
            durationSec = 150
        ),
        BackingTrack(
            id = "moonlight_allegro",
            title = "Moonlight Tragic Allegro",
            artistOrStyle = "Ludwig van Beethoven Style",
            bpm = 132,
            defaultKey = "C#",
            defaultScale = ScaleType.HARMONIC_MINOR,
            recommendedMusicianId = "beethoven",
            sourceType = BackingSourceType.PRESET,
            durationSec = 165
        )
    )

    init {
        activeTrack = builtInTracks.first()
    }

    fun selectTrack(track: BackingTrack) {
        stop()
        activeTrack = track
    }

    fun loadLocalAudio(uri: Uri, fileName: String): BackingTrack {
        stop()
        val isMidi = fileName.endsWith(".mid", ignoreCase = true) || fileName.endsWith(".midi", ignoreCase = true)
        val newTrack = BackingTrack(
            id = "local_${System.currentTimeMillis()}",
            title = fileName.substringBeforeLast("."),
            artistOrStyle = if (isMidi) "Custom MIDI File" else "Imported Audio File",
            bpm = 120,
            defaultKey = "C",
            defaultScale = ScaleType.DORIAN,
            recommendedMusicianId = "eric_johnson",
            sourceType = BackingSourceType.LOCAL_FILE,
            audioUri = uri.toString()
        )
        activeTrack = newTrack
        return newTrack
    }

    fun start() {
        if (isPlaying) return
        val track = activeTrack ?: return
        isPlaying = true

        when (track.sourceType) {
            BackingSourceType.PRESET -> {
                startSynthesizedBacking(track)
            }
            BackingSourceType.LOCAL_FILE -> {
                val uriString = track.audioUri
                if (uriString != null) {
                    val uri = Uri.parse(uriString)
                    if (track.title.endsWith(".mid", ignoreCase = true) || track.artistOrStyle.contains("MIDI")) {
                        startMidiPlayback(uri)
                    } else {
                        startMediaPlayer(uri)
                    }
                } else {
                    startSynthesizedBacking(track)
                }
            }
            BackingSourceType.MIC_LOOP -> {
                startLoopPlayback()
            }
        }
    }

    fun stop() {
        isPlaying = false
        isBackingRunning.set(false)
        backingRenderThread?.interrupt()
        backingRenderThread = null

        try {
            backingAudioTrack?.stop()
            backingAudioTrack?.release()
        } catch (_: Exception) {}
        backingAudioTrack = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    // --- High-Fidelity Musical Arranger Engine for Presets ---
    private fun startSynthesizedBacking(track: BackingTrack) {
        isBackingRunning.set(true)

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        // Generous buffer (16 frames = ~370ms) to ensure smooth playback in browser/emulator virtualized environments
        val bufferBytes = max(minBuf * 2, FRAME_SIZE * 16 * 2)

        backingAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferBytes)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        backingAudioTrack?.play()

        backingRenderThread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            renderMusicalArrangementLoop(track)
        }, "MusicalBackingArrangerThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    private fun renderMusicalArrangementLoop(track: BackingTrack) {
        val buffer = ShortArray(FRAME_SIZE)
        val floatBuf = FloatArray(FRAME_SIZE)

        val bpm = track.bpm
        val beatsPerBar = track.beatsPerBar

        // Exact sample-accurate sequencer math
        val samplesPerBeat = (SAMPLE_RATE * 60.0 / bpm).toInt()
        val samplesPerSubdivision = (samplesPerBeat / 4).coerceAtLeast(1) // 16th note subdivision
        val subdivisionsPerBar = beatsPerBar * 4
        val totalBarsInLoop = when (track.id) {
            "chicago_blues" -> 12 // Full authentic 12-bar blues!
            "baroque_prelude", "cliffs_cascade" -> 8
            else -> 4
        }
        val totalSubdivisions = totalBarsInLoop * subdivisionsPerBar

        // High-Fidelity Wavetable SoundFont Voice Pools
        val drumVoices = Array(12) { WavetableSoundfont.SoundfontDrumVoice() }
        val bassVoice = WavetableSoundfont.AccompanimentWavetableVoice()
        val chordVoices = Array(6) { WavetableSoundfont.AccompanimentWavetableVoice() }

        var currentTotalSubdivisions = 0L
        var sampleCounterInSubdivision = 0

        fun triggerDrum(type: Int, vel: Float = 0.8f) {
            val v = drumVoices.find { !it.active } ?: drumVoices.minByOrNull { it.ageSamples } ?: drumVoices[0]
            v.trigger(type, vel)
        }

        fun triggerBass(midi: Int, durationSubdivisions: Int = 4, table: FloatArray = WavetableSoundfont.electricBassTable, vel: Float = 0.85f) {
            val durationMs = ((durationSubdivisions * samplesPerSubdivision).toDouble() / SAMPLE_RATE * 1000).toInt()
            bassVoice.trigger(midi, durationMs, table, vel, bassMode = true)
        }

        fun triggerChord(
            midiNotes: List<Int>,
            durationSubdivisions: Int = 4,
            table: FloatArray = WavetableSoundfont.rhodesTable,
            vel: Float = 0.70f,
            isStrummed: Boolean = true
        ) {
            val durationMs = ((durationSubdivisions * samplesPerSubdivision).toDouble() / SAMPLE_RATE * 1000).toInt()
            for (idx in 0 until min(midiNotes.size, chordVoices.size)) {
                // Humanized strum delay: micro-offset each note by 10ms for natural string strumming
                val strumOffsetMs = if (isStrummed) idx * 10 else 0
                val noteVel = (vel * (1.0f - idx * 0.04f)).coerceAtLeast(0.5f)
                chordVoices[idx].trigger(midiNotes[idx], (durationMs - strumOffsetMs).coerceAtLeast(50), table, noteVel, bassMode = false)
            }
        }

        var isDownbeatBlock = false

        while (isBackingRunning.get() && !Thread.currentThread().isInterrupted) {
            floatBuf.fill(0f)
            isDownbeatBlock = false

            for (i in 0 until FRAME_SIZE) {
                // Check if we hit a 16th note subdivision boundary
                if (sampleCounterInSubdivision == 0) {
                    val subIndexInLoop = (currentTotalSubdivisions % totalSubdivisions).toInt()
                    val barIndex = subIndexInLoop / subdivisionsPerBar
                    val cumulativeBarIndex = (currentTotalSubdivisions / subdivisionsPerBar).toInt()
                    val subInBar = subIndexInLoop % subdivisionsPerBar
                    val beatIndex = subInBar / 4
                    val subInBeat = subInBar % 4
                    val isTurnaroundBar = (barIndex == totalBarsInLoop - 1)

                    if (subInBar == 0 && subInBeat == 0) {
                        isDownbeatBlock = true
                        // Explosive Crash Cymbal accent on downbeat of new cycle (bar 0)!
                        if (barIndex == 0) {
                            triggerDrum(6, 0.82f) // Crash cymbal
                        }
                    }

                    val currentChordContext = getChordContextForBar(track, cumulativeBarIndex)
                    onSequencerTick?.invoke(cumulativeBarIndex, subInBar, beatIndex, subInBeat == 0, subInBeat == 0 || subInBeat == 2, currentChordContext)

                    // Dynamic Turnaround Drum Fill on final beat of the progression loop
                    if (isTurnaroundBar && beatIndex == (beatsPerBar - 1)) {
                        when (subInBeat) {
                            0 -> triggerDrum(2, 0.85f) // Snare roll
                            1 -> triggerDrum(8, 0.80f) // Tom roll
                            2 -> triggerDrum(2, 0.90f) // Snare accent
                            3 -> triggerDrum(8, 0.95f) // Tom rush leading into next bar!
                        }
                    }

                    // --- HIGH-FIDELITY WAVETABLE SOUNDFONT SEQUENCER PER PRESET ---
                    when (track.id) {
                        "take_five" -> {
                            // 5/4 Swing Jazz Groove (Dave Brubeck Style)
                            // Drums: Ride on every beat; rimshot on beat 4; kick on beat 1 & 3
                            if (subInBeat == 0) {
                                triggerDrum(5, 0.72f) // Sizzling Ride cymbal
                                if (beatIndex == 0) triggerDrum(1, 0.85f) // Studio Kick
                                if (beatIndex == 2) triggerDrum(1, 0.70f)
                                if (beatIndex == 3) triggerDrum(7, 0.80f) // Rimshot accent
                            }
                            // Swing ride pulse on the "&" of beat 1 and 3
                            if ((beatIndex == 0 || beatIndex == 2) && subInBeat == 2) {
                                triggerDrum(5, 0.55f)
                            }
                            // Acoustic Upright Walking Bass in 5/4 (Ebm7)
                            if (subInBeat == 0) {
                                val bassPitch = when (beatIndex) {
                                    0 -> 39 // Eb2
                                    1 -> 42 // Gb2
                                    2 -> 46 // Bb2
                                    3 -> 44 // Ab2
                                    else -> 41 // F2
                                }
                                triggerBass(bassPitch, 4, WavetableSoundfont.uprightBassTable, 0.85f)
                            }
                            // Warm Fender Rhodes comping on beat 1 and beat 4
                            if (subInBeat == 0 && (beatIndex == 0 || beatIndex == 3)) {
                                val chord = listOf(51, 54, 58, 61) // Eb3, Gb3, Bb3, Db4 (Ebm7)
                                triggerChord(chord, 6, WavetableSoundfont.rhodesTable, 0.75f, isStrummed = true)
                            }
                        }

                        "invaders_rave" -> {
                            // 140 BPM Driving Cyber-Rave Breakbeat (The Prodigy Style)
                            // 909 Punchy Kick on 1 and 3, Snappy Snare on 2 and 4
                            if (subInBeat == 0) {
                                if (beatIndex == 0 || beatIndex == 2) triggerDrum(1, 0.95f) // Punchy Kick
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(2, 0.92f) // Snappy Snare
                                triggerDrum(3, 0.65f) // Closed Hat
                            } else if (subInBeat == 2) {
                                // Sizzling Open Hat on the offbeat "&"
                                triggerDrum(4, 0.60f)
                            }
                            // Rolling 16th Sub-bass in F# minor using Rave Synth wavetable
                            val raveBassNotes = intArrayOf(42, 42, 45, 42, 47, 42, 49, 45) // F#2, F#2, A2, F#2, B2, F#2, C#3, A2
                            val bassPitch = raveBassNotes[subInBar % raveBassNotes.size]
                            triggerBass(bassPitch, 2, WavetableSoundfont.raveSynthTable, 0.92f)

                            // Heavy Aggressive Saw Stabs on Beat 1 and beat 3.5
                            if ((beatIndex == 0 && subInBeat == 0) || (beatIndex == 2 && subInBeat == 2)) {
                                val chord = listOf(54, 57, 61) // F#3, A3, C#4 (F#m)
                                triggerChord(chord, 3, WavetableSoundfont.raveSynthTable, 0.85f, isStrummed = false)
                            }
                        }

                        "chicago_blues" -> {
                            // Authentic 12-Bar Blues in A (Shuffle swing)
                            val bluesRoots = intArrayOf(
                                45, 45, 45, 45, // Bars 1-4: A7
                                50, 50,         // Bars 5-6: D7
                                45, 45,         // Bars 7-8: A7
                                52, 50, 45, 52  // Bars 9-12: E7, D7, A7, E7
                            )
                            val root = bluesRoots[barIndex]
                            // Shuffle drums: Kick on 1 & 3, Snare on 2 & 4, Hi-hat shuffle
                            if (subInBeat == 0) {
                                if (beatIndex == 0 || beatIndex == 2) triggerDrum(1, 0.88f)
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(2, 0.85f)
                                triggerDrum(3, 0.60f)
                            } else if (subInBeat == 2) {
                                triggerDrum(3, 0.45f) // Swing shuffle hat
                            }
                            // Classic Fender Jazz Bass Boogie Walk (Root, 3rd, 5th, 6th)
                            if (subInBeat == 0) {
                                val interval = when (beatIndex) {
                                    0 -> 0; 1 -> 4; 2 -> 7; else -> 9
                                }
                                triggerBass(root - 12 + interval, 4, WavetableSoundfont.electricBassTable, 0.88f)
                            }
                            // Offbeat Blues Guitar Chop on "&" of each beat with realistic strum
                            if (subInBeat == 2) {
                                val chord = listOf(root, root + 4, root + 7, root + 10)
                                triggerChord(chord, 2, WavetableSoundfont.overdriveGuitarTable, 0.75f, isStrummed = true)
                            }
                        }

                        "kind_of_blue" -> {
                            // Miles Davis D Dorian "So What"
                            if (subInBeat == 0) {
                                triggerDrum(5, 0.68f) // Soft ride cymbal
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(2, 0.45f) // Brushed snare
                            }
                            if (subInBeat == 0) {
                                val bassWalk = intArrayOf(50, 52, 53, 55, 57, 60, 57, 55)
                                triggerBass(bassWalk[(barIndex * 4 + beatIndex) % bassWalk.size] - 12, 4, WavetableSoundfont.uprightBassTable, 0.82f)
                            }
                            if (beatIndex == 2 && subInBeat == 2) {
                                // Classic Bill Evans So What 4ths Chord on Rhodes
                                val chord = listOf(52, 57, 62, 67, 71) // E3, A3, D4, G4, B4
                                triggerChord(chord, 6, WavetableSoundfont.rhodesTable, 0.70f, isStrummed = true)
                            }
                        }

                        "cliffs_cascade" -> {
                            // Eric Johnson Melodic Rock (G -> C -> Em -> D)
                            if (subInBeat == 0) {
                                if (beatIndex == 0 || beatIndex == 2) triggerDrum(1, 0.88f)
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(2, 0.88f)
                                triggerDrum(3, 0.65f)
                            } else if (subInBeat == 2) {
                                triggerDrum(3, 0.50f)
                            }
                            val gChordRoots = intArrayOf(43, 48, 40, 50) // G, C, E, D
                            val curRoot = gChordRoots[barIndex % gChordRoots.size]
                            if (subInBeat == 0) {
                                triggerBass(curRoot - 12, 4, WavetableSoundfont.electricBassTable, 0.85f)
                            }
                            if (subInBeat == 0 && (beatIndex == 0 || beatIndex == 2)) {
                                val chord = listOf(curRoot, curRoot + 4, curRoot + 7)
                                triggerChord(chord, 4, WavetableSoundfont.overdriveGuitarTable, 0.75f, isStrummed = true)
                            }
                        }

                        "flamenco_compas" -> {
                            // Paco de Lucía Bulerías Compás (180 BPM, E Phrygian Cadence)
                            // Cajon thud on 1 & 3, sharp wood palm slap on 2 & 4
                            if (subInBeat == 0) {
                                if (beatIndex == 0 || beatIndex == 2) triggerDrum(9, 0.90f) // Cajon Thud
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(10, 0.88f) // Cajon Slap
                            }
                            val flamencoCadence = intArrayOf(45, 43, 41, 40) // Am, G, F, E
                            val curRoot = flamencoCadence[barIndex % 4]
                            if (subInBeat == 0) {
                                triggerBass(curRoot - 12, 4, WavetableSoundfont.uprightBassTable, 0.80f)
                            }
                            if (subInBeat == 0 || subInBeat == 2) {
                                val chord = listOf(curRoot, curRoot + 3, curRoot + 7)
                                triggerChord(chord, 3, WavetableSoundfont.flamencoGuitarTable, 0.82f, isStrummed = true)
                            }
                        }

                        "sunset_metal" -> {
                            // Heavy Metal Power Chugs (Van Halen / Randy Rhoads)
                            if (subInBeat == 0) {
                                triggerDrum(1, 0.95f) // Double-kick
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(2, 0.95f) // Heavy Snare
                                triggerDrum(3, 0.70f)
                            } else if (subInBeat == 2) {
                                triggerDrum(1, 0.80f) // Double-kick drive
                            }
                            val metalRoots = intArrayOf(45, 41, 43, 40) // A, F, G, E
                            val curRoot = metalRoots[barIndex % 4]
                            // Palm-muted 16th chug on bass
                            triggerBass(curRoot - 12, 2, WavetableSoundfont.electricBassTable, 0.90f)
                            if (beatIndex == 0 && subInBeat == 0) {
                                val pChord = listOf(curRoot, curRoot + 7, curRoot + 12) // Heavy Power Chord
                                triggerChord(pChord, 6, WavetableSoundfont.highGainGuitarTable, 0.88f, isStrummed = true)
                            }
                        }

                        "pink_ballad" -> {
                            // David Gilmour Slow Ballad (74 BPM)
                            if (subInBeat == 0) {
                                if (beatIndex == 0) triggerDrum(1, 0.88f)
                                if (beatIndex == 2) triggerDrum(1, 0.65f)
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(2, 0.90f) // Big Reverb Snare
                                triggerDrum(3, 0.45f)
                            }
                            val balladRoots = intArrayOf(47, 45, 43, 40) // Bm, A, G, Em
                            val curRoot = balladRoots[barIndex % 4]
                            if (subInBeat == 0 && beatIndex == 0) {
                                triggerBass(curRoot - 12, 16, WavetableSoundfont.electricBassTable, 0.88f)
                                val chord = listOf(curRoot, curRoot + 3, curRoot + 7)
                                triggerChord(chord, 14, WavetableSoundfont.rhodesTable, 0.75f, isStrummed = true)
                            }
                        }

                        "baroque_prelude" -> {
                            // J.S. Bach Baroque Prelude in D Major
                            val bachProgression = intArrayOf(50, 45, 47, 42, 43, 50, 40, 45) // D, A, Bm, F#m, G, D, Em, A
                            val curRoot = bachProgression[barIndex % bachProgression.size]
                            // Continuous 16th note Concert Grand / Harpsichord Arpeggio
                            val arpNotes = intArrayOf(curRoot, curRoot + 4, curRoot + 7, curRoot + 12)
                            val arpPitch = arpNotes[subInBar % arpNotes.size]
                            val hChord = listOf(arpPitch)
                            triggerChord(hChord, 2, WavetableSoundfont.grandPianoTable, 0.78f, isStrummed = false)
                            // Basso Continuo Cello Walking Bass
                            if (subInBeat == 0) {
                                triggerBass(curRoot - 12, 4, WavetableSoundfont.uprightBassTable, 0.82f)
                            }
                        }

                        "moonlight_allegro" -> {
                            // Beethoven Moonlight Allegro (C# Minor)
                            val beetRoots = intArrayOf(49, 44, 49, 41) // C#m, G#, C#m, F#m
                            val curRoot = beetRoots[barIndex % 4]
                            // Classical Concert Grand Piano Triplet Arpeggio
                            val tripNotes = intArrayOf(curRoot, curRoot + 3, curRoot + 7)
                            val pitch = tripNotes[subInBar % tripNotes.size]
                            triggerChord(listOf(pitch), 2, WavetableSoundfont.grandPianoTable, 0.82f, isStrummed = false)
                            if (subInBeat == 0 && beatIndex == 0) {
                                triggerBass(curRoot - 12, 16, WavetableSoundfont.grandPianoTable, 0.90f)
                            }
                        }

                        else -> {
                            // Default Solid Groove
                            if (subInBeat == 0) {
                                if (beatIndex == 0 || beatIndex == 2) triggerDrum(1, 0.85f)
                                if (beatIndex == 1 || beatIndex == 3) triggerDrum(2, 0.85f)
                                triggerDrum(3, 0.55f)
                                triggerBass(45, 4, WavetableSoundfont.electricBassTable, 0.80f)
                            }
                        }
                    }

                    currentTotalSubdivisions++
                }

                sampleCounterInSubdivision = (sampleCounterInSubdivision + 1) % samplesPerSubdivision

                // Sum all drum voices
                var drumSum = 0.0
                for (v in drumVoices) {
                    drumSum += v.nextSample()
                }

                // Sum bass voice
                val bassSum = bassVoice.nextSample()

                // Sum chord voices
                var chordSum = 0.0
                for (c in chordVoices) {
                    chordSum += c.nextSample()
                }

                val effectiveVol = if (isMuted) 0f else volume
                val mixed = (drumSum * 0.78 + bassSum * 0.82 + chordSum * 0.68) * effectiveVol
                floatBuf[i] = mixed.toFloat()
            }

            for (i in 0 until FRAME_SIZE) {
                val x = floatBuf[i]
                val s = when {
                    x > 1.4f -> 0.96f
                    x < -1.4f -> -0.96f
                    else -> x - (x * x * x) * 0.13f
                }
                buffer[i] = (s * 32767f).toInt().coerceIn(-32768, 32767).toShort()
            }

            // Write to AudioTrack with zero buffer underflows
            backingAudioTrack?.write(buffer, 0, FRAME_SIZE)

            // Feed to AudioAnalyzer with exact beat sync!
            onAudioFrameReady(buffer, FRAME_SIZE, isDownbeatBlock)
        }
    }

    fun getChordContextForBar(track: BackingTrack, barIndex: Int): com.example.model.ChordContext {
        return when (track.id) {
            "take_five" -> {
                val roots = intArrayOf(3, 10) // Ebm7, Bbm7
                val names = arrayOf("Ebm7", "Bbm7")
                val idx = (barIndex % 2 + 2) % 2
                com.example.model.ChordContext(roots[idx], true, names[idx])
            }
            "invaders_rave" -> {
                val roots = intArrayOf(6, 6, 9, 1) // F#m, F#m, A, C#m
                val minors = booleanArrayOf(true, true, false, true)
                val names = arrayOf("F#m", "F#m", "A", "C#m")
                val idx = (barIndex % 4 + 4) % 4
                com.example.model.ChordContext(roots[idx], minors[idx], names[idx])
            }
            "chicago_blues" -> {
                val roots = intArrayOf(9, 9, 9, 9, 2, 2, 9, 9, 4, 2, 9, 4)
                val names = arrayOf("A7", "A7", "A7", "A7", "D7", "D7", "A7", "A7", "E7", "D7", "A7", "E7")
                val idx = (barIndex % 12 + 12) % 12
                com.example.model.ChordContext(roots[idx], false, names[idx])
            }
            "kind_of_blue" -> {
                // "So What" 32-bar form: 16 bars Dm7, 8 bars Ebm7, 8 bars Dm7
                val idx = (barIndex % 32 + 32) % 32
                if (idx in 16..23) {
                    com.example.model.ChordContext(3, true, "Ebm7")
                } else {
                    com.example.model.ChordContext(2, true, "Dm7")
                }
            }
            "cliffs_cascade" -> {
                val roots = intArrayOf(7, 0, 4, 2)
                val minors = booleanArrayOf(false, false, true, false)
                val names = arrayOf("G", "C", "Em", "D")
                val idx = (barIndex % 4 + 4) % 4
                com.example.model.ChordContext(roots[idx], minors[idx], names[idx])
            }
            "flamenco_compas" -> {
                val roots = intArrayOf(9, 7, 5, 4)
                val minors = booleanArrayOf(true, false, false, false)
                val names = arrayOf("Am", "G", "F", "E")
                val idx = (barIndex % 4 + 4) % 4
                com.example.model.ChordContext(roots[idx], minors[idx], names[idx])
            }
            "sunset_metal" -> {
                val roots = intArrayOf(9, 5, 7, 4)
                val minors = booleanArrayOf(true, false, false, false)
                val names = arrayOf("Am", "F", "G", "E")
                val idx = (barIndex % 4 + 4) % 4
                com.example.model.ChordContext(roots[idx], minors[idx], names[idx])
            }
            "pink_ballad" -> {
                val roots = intArrayOf(11, 9, 7, 4)
                val minors = booleanArrayOf(true, false, false, true)
                val names = arrayOf("Bm", "A", "G", "Em")
                val idx = (barIndex % 4 + 4) % 4
                com.example.model.ChordContext(roots[idx], minors[idx], names[idx])
            }
            "baroque_prelude" -> {
                val roots = intArrayOf(2, 9, 11, 6, 7, 2, 4, 9)
                val minors = booleanArrayOf(false, false, true, true, false, false, true, false)
                val names = arrayOf("D", "A", "Bm", "F#m", "G", "D", "Em", "A")
                val idx = (barIndex % 8 + 8) % 8
                com.example.model.ChordContext(roots[idx], minors[idx], names[idx])
            }
            "moonlight_allegro" -> {
                val roots = intArrayOf(1, 8, 1, 5)
                val minors = booleanArrayOf(true, false, true, true)
                val names = arrayOf("C#m", "G#", "C#m", "F#m")
                val idx = (barIndex % 4 + 4) % 4
                com.example.model.ChordContext(roots[idx], minors[idx], names[idx])
            }
            else -> com.example.model.ChordContext(
                com.example.engine.MusicTheory.rootNameToMidiRoot(track.defaultKey),
                track.defaultScale != com.example.model.ScaleType.MAJOR,
                track.defaultKey
            )
        }
    }

    // --- Local MIDI Playback ---
    private fun startMidiPlayback(uri: Uri) {
        val stream: InputStream? = try {
            context.contentResolver.openInputStream(uri)
        } catch (_: Exception) { null }

        val parsedMidi = stream?.use { MidiParser.parse(it) }
        if (parsedMidi == null) {
            activeTrack?.let { startSynthesizedBacking(it) }
            return
        }

        isBackingRunning.set(true)
        val minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        backingAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(max(minBuf, FRAME_SIZE * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        backingAudioTrack?.play()

        backingRenderThread = Thread({
            val buffer = ShortArray(FRAME_SIZE)
            val floatBuf = FloatArray(FRAME_SIZE)
            var currentTick = 0L
            val msPerTick = (60000.0 / (parsedMidi.tempoBpm * parsedMidi.ticksPerBeat))
            val samplesPerTick = (SAMPLE_RATE * msPerTick / 1000.0).coerceAtLeast(1.0)
            var sampleCount = 0.0

            val drumVoices = Array(8) { WavetableSoundfont.SoundfontDrumVoice() }
            val melVoices = Array(12) { WavetableSoundfont.AccompanimentWavetableVoice() }

            fun triggerMidiDrum(note: Int, vel: Float) {
                val drumType = when (note) {
                    35, 36 -> 1 // Kick
                    38, 40 -> 2 // Snare
                    37 -> 7     // Rimshot
                    42, 44 -> 3 // Closed Hat
                    46 -> 4     // Open Hat
                    49, 57 -> 6 // Crash
                    51, 59 -> 5 // Ride
                    41, 43, 45, 47, 48, 50 -> 8 // Toms
                    else -> 2
                }
                val v = drumVoices.find { !it.active } ?: drumVoices.minByOrNull { it.ageSamples } ?: drumVoices[0]
                v.trigger(drumType, vel)
            }

            fun triggerMidiMelodic(note: Int, vel: Float) {
                val v = melVoices.find { !it.active } ?: melVoices.minByOrNull { it.ageSamples } ?: melVoices[0]
                val table = if (note < 50) WavetableSoundfont.electricBassTable else WavetableSoundfont.grandPianoTable
                v.trigger(note, 600, table, vel, bassMode = (note < 50))
            }

            var eventIndex = 0

            while (isBackingRunning.get() && !Thread.currentThread().isInterrupted) {
                floatBuf.fill(0f)

                for (i in 0 until FRAME_SIZE) {
                    sampleCount++
                    if (sampleCount >= samplesPerTick) {
                        sampleCount = 0.0
                        currentTick++

                        while (eventIndex < parsedMidi.events.size && parsedMidi.events[eventIndex].timeTick <= currentTick) {
                            val evt = parsedMidi.events[eventIndex]
                            if (evt.type == 0x90 && evt.data2 > 0) {
                                val vel = (evt.data2 / 127f).coerceIn(0.1f, 1f)
                                if (evt.channel == 9) {
                                    triggerMidiDrum(evt.data1, vel)
                                } else {
                                    triggerMidiMelodic(evt.data1, vel)
                                }
                            }
                            eventIndex++
                        }

                        if (eventIndex >= parsedMidi.events.size) {
                            eventIndex = 0
                            currentTick = 0L
                        }
                    }

                    var s = 0.0
                    for (dv in drumVoices) s += dv.nextSample()
                    for (mv in melVoices) s += mv.nextSample()
                    floatBuf[i] = (s * volume).toFloat()
                }

                for (i in 0 until FRAME_SIZE) {
                    val s = tanh(floatBuf[i].toDouble()).toFloat()
                    buffer[i] = (s * 32767f).toInt().coerceIn(-32768, 32767).toShort()
                }

                backingAudioTrack?.write(buffer, 0, FRAME_SIZE)
                onAudioFrameReady(buffer, FRAME_SIZE, false)
            }
        }, "MidiPlaybackThread").apply {
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    // --- Android MediaPlayer for MP3, OGG, WAV ---
    private fun startMediaPlayer(uri: Uri) {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        } catch (_: Exception) {
            activeTrack?.let { startSynthesizedBacking(it) }
        }
    }

    // --- Live Microphone Looper ---
    fun startMicRecording(): Boolean {
        try {
            val minBuf = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                max(minBuf, FRAME_SIZE * 4)
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                return false
            }

            recordedBuffer.clear()
            isMicRecording.set(true)
            audioRecord?.startRecording()

            micRecordThread = Thread({
                val tempBuf = ShortArray(FRAME_SIZE)
                while (isMicRecording.get() && !Thread.currentThread().isInterrupted) {
                    val read = audioRecord?.read(tempBuf, 0, FRAME_SIZE) ?: 0
                    if (read > 0) {
                        synchronized(recordedBuffer) {
                            for (j in 0 until read) {
                                recordedBuffer.add(tempBuf[j])
                            }
                        }
                        onAudioFrameReady(tempBuf, read, false)
                    }
                }
            }, "MicRecorderThread").apply {
                start()
            }
            return true
        } catch (_: Exception) {
            return false
        }
    }

    fun stopMicRecordingAndLoop(): BackingTrack? {
        isMicRecording.set(false)
        micRecordThread?.interrupt()
        micRecordThread = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        synchronized(recordedBuffer) {
            if (recordedBuffer.size < SAMPLE_RATE / 2) return null
            loopedAudioBuffer = recordedBuffer.toShortArray()
        }

        loopPlayIndex = 0
        val durationSec = ((loopedAudioBuffer?.size ?: 0) / SAMPLE_RATE).coerceAtLeast(1)

        val micTrack = BackingTrack(
            id = "mic_loop_${System.currentTimeMillis()}",
            title = "Live Mic Loop",
            artistOrStyle = "User Captured Jam",
            bpm = 120,
            defaultKey = "A",
            defaultScale = ScaleType.DORIAN,
            recommendedMusicianId = "eric_johnson",
            sourceType = BackingSourceType.MIC_LOOP,
            durationSec = durationSec
        )
        activeTrack = micTrack
        start()
        return micTrack
    }

    private fun startLoopPlayback() {
        val loop = loopedAudioBuffer ?: return
        isBackingRunning.set(true)

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        backingAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SAMPLE_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(max(minBuf, FRAME_SIZE * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        backingAudioTrack?.play()

        backingRenderThread = Thread({
            val buffer = ShortArray(FRAME_SIZE)
            while (isBackingRunning.get() && !Thread.currentThread().isInterrupted) {
                for (i in 0 until FRAME_SIZE) {
                    val s = (loop[loopPlayIndex] * volume).toInt().toShort()
                    buffer[i] = s
                    loopPlayIndex = (loopPlayIndex + 1) % loop.size
                }
                backingAudioTrack?.write(buffer, 0, FRAME_SIZE)
                onAudioFrameReady(buffer, FRAME_SIZE, false)
            }
        }, "MicLoopPlaybackThread").apply {
            start()
        }
    }
}
