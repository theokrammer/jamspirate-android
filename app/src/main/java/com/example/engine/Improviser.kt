package com.example.engine

import com.example.audio.PolySynth
import com.example.model.ChordContext
import com.example.model.JamNoteEvent
import com.example.model.MusicianProfile
import com.example.model.ScaleType
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

class Improviser(
    private val synth: PolySynth,
    private val onNoteEvent: (JamNoteEvent) -> Unit
) {
    private var workerThread: Thread? = null
    private val isRunning = AtomicBoolean(false)
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    // Current improvisation context
    @Volatile var activeMusician: MusicianProfile = com.example.model.MusicianRegistry.getById("eric_johnson")
    @Volatile var currentBpm: Int = 120
    @Volatile var currentKey: String = "A"
    @Volatile var currentScale: ScaleType = ScaleType.DORIAN
    @Volatile var densityMultiplier: Float = 1.0f // 0.2 to 2.0
    @Volatile var creativity: Float = 0.5f         // 0.0 to 1.0
    @Volatile var octaveShift: Int = 0             // -1, 0, +1
    @Volatile var isPaused: Boolean = false

    // Sequencer clock tracking
    @Volatile var lastSequencerTickTime: Long = 0L

    // State memory for melodic direction & motif coherence
    private var previousMidiNote: Int = 60
    private var melodicDirection: Int = 1 // 1 for ascending, -1 for descending
    private var motifLength: Int = 4
    private var motifCounter: Int = 0
    private var isCurrentlyResting: Boolean = false
    private var noteIdSequence = 1L

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        workerThread = Thread({ fallbackImprovisationLoop() }, "JamspirateImproviserThread").apply {
            priority = Thread.NORM_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        workerThread?.interrupt()
        workerThread = null
        synth.allNotesOff()
    }

    enum class AudioOutputSource { LOCAL, CLOUD }
    @Volatile var audioOutputSource: AudioOutputSource = AudioOutputSource.LOCAL
    @Volatile var isLocalBackupEnabled: Boolean = true
    var isCloudLookaheadEnabled: Boolean = true
    private val lookaheadBuffer = java.util.concurrent.ConcurrentHashMap<String, com.example.ai.LookaheadNote>()
    private val barsBuffered = java.util.concurrent.ConcurrentHashMap.newKeySet<Int>()

    fun enqueueLookaheadNotes(notes: List<com.example.ai.LookaheadNote>) {
        for (note in notes) {
            lookaheadBuffer["${note.barIndex}:${note.subInBar}"] = note
            barsBuffered.add(note.barIndex)
        }
    }

    fun clearLookaheadBuffer() {
        lookaheadBuffer.clear()
        barsBuffered.clear()
    }

    fun getBufferedNoteCount(): Int = lookaheadBuffer.size

    fun generateLocalBatch(
        upcomingBars: List<Pair<Int, ChordContext>>,
        musician: MusicianProfile,
        key: String,
        scale: ScaleType,
        densityMultiplier: Float,
        creativity: Float,
        octaveShift: Int
    ): List<com.example.ai.LookaheadNote> {
        val result = mutableListOf<com.example.ai.LookaheadNote>()
        var runningNote = 60 + (octaveShift * 12)

        val density = (musician.noteDensity * densityMultiplier).coerceIn(0.2f, 1.3f)
        val step = when {
            density > 0.82f -> 1 // 16th notes
            density > 0.45f -> 2 // 8th notes
            else -> 4           // Quarter notes
        }

        for ((barIndex, chordContext) in upcomingBars) {
            val scaleNotes = MusicTheory.getScaleNotes(
                chordContext.rootPitchClass,
                scale,
                octaveRange = (3 + octaveShift)..(5 + octaveShift)
            ).ifEmpty {
                MusicTheory.getScaleNotes(
                    MusicTheory.rootNameToMidiRoot(key),
                    scale,
                    (3 + octaveShift)..(5 + octaveShift)
                )
            }

            for (sub in 0 until 16 step step) {
                // Rest probability based on musician style
                val restChance = (musician.restProbability * (1.1f - density)).coerceIn(0.04f, 0.45f)
                if (Random.nextFloat() < restChance && sub != 0) {
                    continue
                }

                val note = if (sub == 0 || sub == 8) {
                    // Strong beats: anchor to chord triad tones
                    val chordRootMidi = 48 + chordContext.rootPitchClass + (octaveShift * 12)
                    val triadCandidates = listOf(
                        chordRootMidi,
                        chordRootMidi + (if (chordContext.isMinor) 3 else 4),
                        chordRootMidi + 7,
                        chordRootMidi + 12
                    )
                    triadCandidates.random()
                } else {
                    // Melodic stepwise motion within the scale
                    val stepCandidates = scaleNotes.filter { kotlin.math.abs(it - runningNote) in 1..4 }
                    if (stepCandidates.isNotEmpty()) stepCandidates.random()
                    else scaleNotes.minByOrNull { kotlin.math.abs(it - runningNote) } ?: runningNote
                }

                runningNote = note
                val vel = if (sub == 0) 0.92f else calculateVelocity(musician)
                result.add(
                    com.example.ai.LookaheadNote(
                        barIndex = barIndex,
                        subInBar = sub,
                        midiPitch = note.coerceIn(40, 90),
                        durationSubdivisions = step,
                        velocity = vel
                    )
                )
            }
        }
        return result
    }

    // Called on every 16th-note subdivision from the sample-accurate BackingPlayer sequencer
    fun onSequencerTick(
        barIndex: Int,
        subInBar: Int,
        beatInBar: Int,
        isBeat: Boolean,
        is8thNote: Boolean,
        chordContext: ChordContext
    ) {
        if (isPaused || !isRunning.get()) return
        lastSequencerTickTime = System.currentTimeMillis()

        val musician = activeMusician
        val bpm = currentBpm.coerceIn(50, 240)
        val beatMs = (60000.0 / bpm).toLong()
        val subMs = (beatMs / 4).coerceAtLeast(40L)

        // 1. Diagnostic Influence 1: Pure Root Note of the CURRENT KEY on every beat
        if (musician.id == "diagnostic_root") {
            if (isBeat) {
                val keyRootPitchClass = MusicTheory.rootNameToMidiRoot(currentKey)
                val rootMidi = 48 + keyRootPitchClass + (octaveShift * 12)
                val durationMs = (beatMs * 0.82).toLong().coerceAtLeast(80L)
                triggerNoteDirect(rootMidi, 0.92f, durationMs, false)
            }
            return
        }

        // 2. Diagnostic Influence 2: Alternates Chord Root & 3rd as Eighth Notes
        if (musician.id == "diagnostic_root_third") {
            if (is8thNote) {
                val chordRootMidi = 48 + chordContext.rootPitchClass + (octaveShift * 12)
                val subInBeat = subInBar % 4
                val note = if (subInBeat == 0) {
                    chordRootMidi
                } else {
                    chordRootMidi + (if (chordContext.isMinor) 3 else 4)
                }
                val durationMs = (beatMs * 0.42).toLong().coerceAtLeast(60L)
                triggerNoteDirect(note, 0.88f, durationMs, false)
            }
            return
        }

        // 3. Local generation ALWAYS runs continuously
        val density = (musician.noteDensity * densityMultiplier).coerceIn(0.1f, 1.4f)
        val shouldTriggerLocal = when {
            density > 0.82f -> true // 16th note cascade
            density > 0.45f -> is8thNote // 8th note lines
            else -> isBeat // Spacious quarter notes
        }

        val restChance = (musician.restProbability * (1.2f - density)).coerceIn(0.04f, 0.60f)
        val isLocalResting = !shouldTriggerLocal || (Random.nextFloat() < restChance)

        val localMidiNote: Int? = if (!isLocalResting) {
            val rootPitchClass = chordContext.rootPitchClass
            val scaleNotes = MusicTheory.getScaleNotes(
                rootPitchClass,
                currentScale,
                octaveRange = (3 + octaveShift)..(5 + octaveShift)
            )
            val nextNote = selectNextNote(scaleNotes, musician)
            previousMidiNote = nextNote
            nextNote
        } else null

        val localVelocity = if (localMidiNote != null) calculateVelocity(musician) else 0f
        val localDurationMs = when {
            density > 0.82f -> (beatMs * 0.22).toLong()
            density > 0.45f -> (beatMs * 0.42).toLong()
            else -> (beatMs * 0.80).toLong()
        }.coerceAtLeast(60L)
        val localIsBend = Random.nextFloat() < musician.bendTendency

        // 4. Audio output routing:
        // Selecting the cloud model only changes the audio output destination!
        val key = "$barIndex:$subInBar"
        val plannedCloudNote = lookaheadBuffer.remove(key)

        if (audioOutputSource == AudioOutputSource.CLOUD) {
            if (plannedCloudNote != null) {
                val durationMs = (plannedCloudNote.durationSubdivisions * subMs).coerceAtLeast(50L)
                val noteWithOctave = (plannedCloudNote.midiPitch + octaveShift * 12).coerceIn(36, 96)
                triggerNoteDirect(noteWithOctave, plannedCloudNote.velocity, durationMs, false)
                return
            } else if (barsBuffered.contains(barIndex)) {
                // Cloud specifically scheduled a rest on this tick
                return
            }
            // If cloud buffer underran or is buffering, fall back to local note only if backup is enabled
            if (isLocalBackupEnabled && localMidiNote != null) {
                triggerNoteDirect(localMidiNote, localVelocity, localDurationMs, localIsBend)
            }
            return
        }

        // Local theory engine audio output
        if (localMidiNote != null) {
            triggerNoteDirect(localMidiNote, localVelocity, localDurationMs, localIsBend)
        }
    }

    private fun triggerNoteDirect(
        midiNote: Int,
        velocity: Float,
        durationMs: Long,
        isBend: Boolean
    ) {
        synth.noteOn(midiNote, velocity)

        if (isBend) {
            synth.pitchBendSemitones = Random.nextFloat() * 1.5f
        } else {
            synth.pitchBendSemitones = 0f
        }

        val noteName = MusicTheory.midiToNoteName(midiNote)
        val freq = MusicTheory.midiToFrequency(midiNote.toFloat())

        onNoteEvent(
            JamNoteEvent(
                id = noteIdSequence++,
                midiNote = midiNote,
                noteName = noteName,
                frequencyHz = freq,
                startTimeMs = System.currentTimeMillis(),
                durationMs = durationMs,
                velocity = velocity,
                isBend = isBend,
                musicianName = activeMusician.name
            )
        )

        // Schedule NoteOff cleanly without creating endless threads
        try {
            scheduler.schedule({
                synth.noteOff(midiNote)
                if (isBend) synth.pitchBendSemitones = 0f
            }, durationMs, TimeUnit.MILLISECONDS)
        } catch (_: Exception) {}
    }

    // Fallback loop only when no sequencer clock is active (e.g. standalone soloing)
    private fun fallbackImprovisationLoop() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            if (isPaused) {
                synth.allNotesOff()
                try {
                    Thread.sleep(80L)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            // If the sample-accurate sequencer is actively driving ticks, let the sequencer clock lead!
            if (System.currentTimeMillis() - lastSequencerTickTime < 800) {
                try {
                    Thread.sleep(50L)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            // Standalone mode: fallback timer
            val bpm = currentBpm.coerceIn(50, 240)
            val musician = activeMusician
            val beatMs = (60000.0 / bpm).toLong()

            if (musician.id == "diagnostic_root") {
                val keyRootPitchClass = MusicTheory.rootNameToMidiRoot(currentKey)
                val rootMidi = 48 + keyRootPitchClass + (octaveShift * 12)
                val durationMs = (beatMs * 0.82).toLong().coerceAtLeast(80L)
                triggerNoteDirect(rootMidi, 0.92f, durationMs, false)
                try {
                    Thread.sleep(beatMs)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            if (musician.id == "diagnostic_root_third") {
                val keyRootPitchClass = MusicTheory.rootNameToMidiRoot(currentKey)
                val baseMidi = 48 + keyRootPitchClass + (octaveShift * 12)
                val isMinor = currentScale != ScaleType.MAJOR && currentScale != ScaleType.MAJOR_PENTATONIC && currentScale != ScaleType.MIOLYDIAN
                val eighthMs = (beatMs / 2).coerceAtLeast(70L)

                // 1. Root on the beat
                triggerNoteDirect(baseMidi, 0.88f, (eighthMs * 0.80).toLong().coerceAtLeast(60L), false)
                try {
                    Thread.sleep(eighthMs)
                } catch (_: InterruptedException) {
                    break
                }

                // 2. Minor/Major 3rd on the offbeat
                val thirdMidi = baseMidi + (if (isMinor) 3 else 4)
                triggerNoteDirect(thirdMidi, 0.88f, (eighthMs * 0.80).toLong().coerceAtLeast(60L), false)
                try {
                    Thread.sleep(eighthMs)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            val density = (musician.noteDensity * densityMultiplier).coerceIn(0.1f, 1.2f)
            val subdivision = determineSubdivision(musician, density, beatMs)

            val rootMidiPitchClass = MusicTheory.rootNameToMidiRoot(currentKey)
            val scaleNotes = MusicTheory.getScaleNotes(
                rootMidiPitchClass,
                currentScale,
                octaveRange = (3 + octaveShift)..(5 + octaveShift)
            )

            val nextNote = selectNextNote(scaleNotes, musician)
            previousMidiNote = nextNote
            val isBend = Random.nextFloat() < musician.bendTendency
            val velocity = calculateVelocity(musician)
            val noteDurationMs = (subdivision * 0.85).toLong().coerceAtLeast(60L)

            triggerNoteDirect(nextNote, velocity, noteDurationMs, isBend)

            try {
                Thread.sleep(subdivision)
            } catch (_: InterruptedException) {
                break
            }
        }
    }

    private fun determineSubdivision(musician: MusicianProfile, density: Float, beatMs: Long): Long {
        return when {
            density > 0.85f -> (beatMs / 4).coerceAtLeast(70L)
            density > 0.50f -> (beatMs / 2).coerceAtLeast(130L)
            else -> beatMs.coerceAtLeast(240L)
        }
    }

    private fun selectNextNote(scaleNotes: List<Int>, musician: MusicianProfile): Int {
        if (scaleNotes.isEmpty()) return 60 + (octaveShift * 12)

        val outsideChance = (musician.outsideNoteTendency * (creativity * 1.5f)).coerceIn(0.0f, 0.40f)
        val isOutside = Random.nextFloat() < outsideChance

        if (isOutside) {
            val nearest = scaleNotes.minByOrNull { kotlin.math.abs(it - previousMidiNote) } ?: 60
            return (nearest + if (Random.nextBoolean()) 1 else -1).coerceIn(40, 92)
        }

        val isArpeggio = Random.nextFloat() < musician.arpeggioTendency
        val candidateNotes = if (isArpeggio) {
            scaleNotes.filterIndexed { index, _ -> index % 2 == 0 }
        } else {
            scaleNotes
        }

        motifCounter++
        if (motifCounter > motifLength) {
            motifCounter = 0
            melodicDirection = if (Random.nextBoolean()) 1 else -1
            motifLength = Random.nextInt(3, 7)
        }

        val filtered = candidateNotes.filter {
            if (melodicDirection > 0) it >= previousMidiNote else it <= previousMidiNote
        }

        val pool = if (filtered.isNotEmpty()) filtered else candidateNotes
        return pool.minByOrNull { kotlin.math.abs(it - (previousMidiNote + melodicDirection * 3)) }
            ?: pool.random()
    }

    private fun calculateVelocity(musician: MusicianProfile): Float {
        return when (musician.id) {
            "beethoven" -> if (Random.nextBoolean()) 0.95f else 0.40f
            "miles_davis" -> Random.nextDouble(0.55, 0.80).toFloat()
            "eddie_van_halen", "randy_rhoads" -> Random.nextDouble(0.78, 0.98).toFloat()
            "paco_de_lucia" -> Random.nextDouble(0.70, 0.95).toFloat()
            "mark_knopfler" -> Random.nextDouble(0.50, 0.75).toFloat()
            else -> Random.nextDouble(0.65, 0.90).toFloat()
        }
    }
}
