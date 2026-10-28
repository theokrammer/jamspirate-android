package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.example.ai.CloudLlmClient
import com.example.ai.LlmProvider
import com.example.ai.LookaheadNote
import com.example.audio.AudioAnalyzer
import com.example.audio.BackingPlayer
import com.example.audio.PolySynth
import com.example.engine.Improviser
import com.example.engine.MusicTheory
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.example.comparison.ComparisonBatchEngine
import com.example.comparison.ComparisonSession
import com.example.comparison.ComparisonVariant
import com.example.comparison.ComparisonVariantResult

data class StudioUiState(
    val isJamming: Boolean = false,
    val isMicRecording: Boolean = false,
    val activeTrack: BackingTrack,
    val activeMusician: MusicianProfile,
    val activeInstrument: LeadInstrument,
    val detectedPitchHz: Float = 0f,
    val detectedNoteName: String = "--",
    val detectedCents: Float = 0f,
    val detectedConfidence: Float = 0f,
    val detectedBpm: Int = 120,
    val beatPulse: Boolean = false,
    val detectedKey: String = "A",
    val detectedScale: ScaleType = ScaleType.DORIAN,
    val soloDensity: Float = 1.0f,
    val creativity: Float = 0.5f,
    val octaveShift: Int = 0,
    val timeSignature: String = "4/4",
    val beatsPerBar: Int = 4,
    // Mixer Console State
    val masterVolume: Float = 0.85f,
    val backingVolume: Float = 0.80f,
    val soloVolume: Float = 0.90f,
    val crossfaderBalance: Float = 0.5f,
    val isSoloMuted: Boolean = false,
    val isBackingMuted: Boolean = false,
    // Cloud LLM Lookahead Batching State
    val activeLlmProvider: LlmProvider = LlmProvider.GEMINI,
    val activeResolvedModel: String = "gemini-2.5-flash",
    val isLookaheadBatchingEnabled: Boolean = true,
    val geminiApiKey: String = "",
    val openAiApiKey: String = "",
    val claudeApiKey: String = "",
    val llmStatusMessage: String = "Google Gemini Active",
    val isLlmFetching: Boolean = false,
    val bufferedNoteCount: Int = 0,
    // Synthesizer State
    val driveAmount: Float = 0.5f,
    val filterCutoff: Float = 0.7f,
    val vibratoAmount: Float = 0.35f,
    val recentNotes: List<JamNoteEvent> = emptyList(),
    val oscilloscopeWaveform: FloatArray = FloatArray(128),
    // Standard Music Notation & Batch Comparison State
    val localBatchedNotes: List<com.example.ai.LookaheadNote> = emptyList(),
    val cloudBatchedNotes: List<com.example.ai.LookaheadNote> = emptyList(),
    val batchedBarRange: Pair<Int, Int> = Pair(1, 2),
    val batchedChordNames: Pair<String, String> = Pair("", ""),
    val currentPlayingBar: Int = 0,
    val currentPlayingSub: Int = 0,
    val audioOutputDestination: com.example.ai.AudioOutputDestination = com.example.ai.AudioOutputDestination.CLOUD,
    val isLocalBackupEnabled: Boolean = true,
    val useFamousPiecePromptMode: Boolean = false,
    val isNotationExpanded: Boolean = true,
    val isMixerExpanded: Boolean = false,
    val customAiPromptDirective: String = "",
    // Comparison Matrix & Gallery State
    val showComparisonMatrixDialog: Boolean = false,
    val showComparisonGalleryDialog: Boolean = false,
    val activeComparisonSession: ComparisonSession? = null,
    val comparisonResults: List<ComparisonVariantResult> = emptyList(),
    val isComparisonGenerating: Boolean = false,
    val isAuditioning: Boolean = false,
    val auditionVariantId: String? = null
)

class StudioViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("jamspirate_prefs", Context.MODE_PRIVATE)
    private val cloudLlmClient = CloudLlmClient()
    private val localNotesCache = java.util.concurrent.ConcurrentHashMap<Int, List<LookaheadNote>>()
    private val cloudNotesCache = java.util.concurrent.ConcurrentHashMap<Int, List<LookaheadNote>>()
    private var lastPrefetchedBar = -1

    private val synth = PolySynth()
    private val analyzer = AudioAnalyzer(PolySynth.SAMPLE_RATE)
    private val backingPlayer: BackingPlayer = BackingPlayer(application) { samples, length, isDownbeat ->
        val currentTrack = _uiState.value.activeTrack
        if (currentTrack.sourceType == BackingSourceType.PRESET) {
            // Preset tracks have rock-solid known musical parameters: only pulse downbeat in UI to avoid Main thread flooding
            if (isDownbeat) {
                val rootPitchClass = MusicTheory.rootNameToMidiRoot(currentTrack.defaultKey)
                val rootMidi = 48 + rootPitchClass
                val rootFreq = MusicTheory.midiToFrequency(rootMidi.toFloat())
                viewModelScope.launch(Dispatchers.Main.immediate) {
                    _uiState.update { current ->
                        current.copy(
                            detectedPitchHz = rootFreq,
                            detectedNoteName = currentTrack.defaultKey,
                            detectedCents = 0f,
                            detectedConfidence = 1.0f,
                            detectedBpm = currentTrack.bpm,
                            beatPulse = true,
                            detectedKey = currentTrack.defaultKey,
                            detectedScale = currentTrack.defaultScale
                        )
                    }
                    delay(100)
                    _uiState.update { it.copy(beatPulse = false) }
                }
            }
        } else {
            // Live Mic or external audio: process in background worker coroutine so audio thread never drops frames
            viewModelScope.launch(Dispatchers.Default) {
                val result = analyzer.processAudioFrame(samples, length)
                handleAnalysisResult(result, isDownbeat)
            }
        }
    }

    val allBuiltInTracks: List<BackingTrack>
        get() = backingPlayer.builtInTracks

    private val improviser: Improviser = Improviser(synth) { noteEvent ->
        viewModelScope.launch(Dispatchers.Main.immediate) {
            _uiState.update { current ->
                val updatedNotes = (listOf(noteEvent) + current.recentNotes).take(12)
                current.copy(
                    recentNotes = updatedNotes,
                    bufferedNoteCount = improviser.getBufferedNoteCount()
                )
            }
        }
    }

    private val comparisonBatchEngine = ComparisonBatchEngine(cloudLlmClient, improviser)
    private var auditionJob: kotlinx.coroutines.Job? = null

    private val initialTrack = backingPlayer.builtInTracks.first()
    private val initialMusician = MusicianRegistry.getById(initialTrack.recommendedMusicianId)

    private val _uiState: MutableStateFlow<StudioUiState> = MutableStateFlow(
        StudioUiState(
            activeTrack = initialTrack,
            timeSignature = initialTrack.timeSignature,
            beatsPerBar = initialTrack.beatsPerBar,
            activeMusician = initialMusician,
            activeInstrument = initialMusician.defaultInstrument,
            detectedBpm = initialTrack.bpm,
            activeLlmProvider = try {
                val saved = prefs.getString("llm_provider", LlmProvider.GEMINI.name) ?: LlmProvider.GEMINI.name
                val parsed = LlmProvider.valueOf(saved)
                if (parsed == LlmProvider.LOCAL_THEORY) LlmProvider.GEMINI else parsed
            } catch (_: Exception) { LlmProvider.GEMINI },
            isLookaheadBatchingEnabled = true,
            geminiApiKey = prefs.getString("gemini_key", "").takeIf { !it.isNullOrBlank() }
                ?: try { com.example.BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() && !it.startsWith("MY_") } } catch (_: Throwable) { null }
                ?: System.getenv("GEMINI_API_KEY")?.takeIf { it.isNotBlank() }
                ?: "",
            openAiApiKey = prefs.getString("openai_key", "") ?: "",
            claudeApiKey = prefs.getString("claude_key", "") ?: "",
            isLocalBackupEnabled = prefs.getBoolean("local_backup_enabled", true),
            useFamousPiecePromptMode = prefs.getBoolean("use_famous_piece_prompt", false),
            customAiPromptDirective = prefs.getString("custom_ai_prompt", "") ?: ""
        )
    )
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        // Configure initial improviser lookahead setting, audio routing, and backup mode
        val initialProvider = _uiState.value.activeLlmProvider
        val isInitialCloud = initialProvider != LlmProvider.LOCAL_THEORY
        val initialDestination = if (isInitialCloud) com.example.ai.AudioOutputDestination.CLOUD else com.example.ai.AudioOutputDestination.LOCAL
        improviser.audioOutputSource = if (isInitialCloud) Improviser.AudioOutputSource.CLOUD else Improviser.AudioOutputSource.LOCAL
        improviser.isCloudLookaheadEnabled = isInitialCloud
        improviser.isLocalBackupEnabled = _uiState.value.isLocalBackupEnabled
        _uiState.update { it.copy(audioOutputDestination = initialDestination) }

        // Wire sample-accurate sequencer clock from BackingPlayer to Improviser
        backingPlayer.onSequencerTick = { barIndex, subInBar, beatInBar, isBeat, is8thNote, chordContext ->
            val track = _uiState.value.activeTrack
            val pageStartBar = (barIndex / 2) * 2
            val displayBar1 = pageStartBar + 1
            val displayBar2 = pageStartBar + 2
            val chord1 = backingPlayer.getChordContextForBar(track, pageStartBar)
            val chord2 = backingPlayer.getChordContextForBar(track, pageStartBar + 1)

            val currentLocalNotes = (localNotesCache[pageStartBar] ?: emptyList()) + (localNotesCache[pageStartBar + 1] ?: emptyList())
            val currentCloudNotes = (cloudNotesCache[pageStartBar] ?: emptyList()) + (cloudNotesCache[pageStartBar + 1] ?: emptyList())

            // Keep UI playhead, dynamic bar headers, and rolling notes continuously updated
            _uiState.update {
                it.copy(
                    currentPlayingBar = barIndex,
                    currentPlayingSub = subInBar,
                    batchedBarRange = Pair(displayBar1, displayBar2),
                    batchedChordNames = Pair(chord1.chordName, chord2.chordName),
                    localBatchedNotes = currentLocalNotes,
                    cloudBatchedNotes = currentCloudNotes
                )
            }

            // Local generation ALWAYS runs continuously
            improviser.onSequencerTick(barIndex, subInBar, beatInBar, isBeat, is8thNote, chordContext)

            // Option B: Predictive Early Prefetching
            // When reaching the 2nd bar of current page (barIndex == pageStartBar + 1),
            // trigger lookahead generation for the NEXT 2-bar page early
            val nextUpcomingPageStart = pageStartBar + 2
            if (barIndex >= pageStartBar + 1 && lastPrefetchedBar < nextUpcomingPageStart + 1) {
                prefetchBars(listOf(nextUpcomingPageStart, nextUpcomingPageStart + 1))
            }
        }

        // Pre-compute initial 2-bar batch and prefetch the next page immediately
        prefetchBars(listOf(0, 1, 2, 3))
    }

    fun getActiveTheoryAndPromptExplanation(): String {
        val state = _uiState.value
        val isDiagnostic = state.activeMusician.id.startsWith("diagnostic")

        return """
================================================================================
JAMSPIRATE! REAL-TIME IMPROVISATION ENGINE & PROMPT SPECIFICATION
================================================================================

1. ACTIVE SYSTEM PROMPT USED FOR NOTE GENERATION:
--------------------------------------------------------------------------------
"System: You are an elite lead improviser channeling ${state.activeMusician.name}.
You are improvising in real-time over the backing track '${state.activeTrack.title}' (${state.activeTrack.artistOrStyle}).

TRACK CONTEXT:
• Tempo: ${state.detectedBpm} BPM (Quarter Note = ${(60000.0 / state.detectedBpm).toInt()} ms, Eighth Note = ${(30000.0 / state.detectedBpm).toInt()} ms, Sixteenth = ${(15000.0 / state.detectedBpm).toInt()} ms)
• Key Center: ${state.detectedKey}
• Harmonic Scale: ${state.detectedScale.displayName}
• Scale Semitone Intervals: [${state.detectedScale.intervals.joinToString(", ")}]
• Lead Instrument: ${state.activeInstrument.displayName} (${state.activeInstrument.category})

MUSICIAN STYLE PROFILE (${state.activeMusician.name}):
• Category: ${state.activeMusician.category}
• Signature Phrasing: ${state.activeMusician.signatureStyle}
• Phrasing Density: ${(state.activeMusician.noteDensity * state.soloDensity * 100).toInt()}% (${if (state.activeMusician.noteDensity > 0.8) "16th-note cascades" else if (state.activeMusician.noteDensity > 0.45) "8th-note syncopated lines" else "Spacious quarter-note phrasing"})
• Swing Groove: ${(state.activeMusician.swingFactor * 100).toInt()}%
• Rest / Breathing Tendency: ${(state.activeMusician.restProbability * 100).toInt()}%
• Pitch Bend & Slide Tendency: ${(state.activeMusician.bendTendency * 100).toInt()}%
• Arpeggio Tendency: ${(state.activeMusician.arpeggioTendency * 100).toInt()}%
• Chromatic Tension / Out-Notes: ${(state.activeMusician.outsideNoteTendency * state.creativity * 100).toInt()}%

EXECUTION DIRECTIVE:
${if (isDiagnostic) """
⚠️ DIAGNOSTIC MODE ENGAGED (AI BYPASS):
• The generation engine bypasses all probabilistic heuristics.
• Active Test: ${state.activeMusician.name}
• Purpose: If this mode sounds in-time and consonant, the audio clock and harmonic root are verified. Any perceived dissonance in other presets stems from high chromatic creativity or high note density.
""" else """
• Standard Mode: Channel ${state.activeMusician.name} phrasing using modal target notes, directional motifs, dynamic velocity articulation, and responsive chord tracking.
"""}
"

--------------------------------------------------------------------------------
2. DIAGNOSTIC INFLUENCES EXPLANATION:
--------------------------------------------------------------------------------
• 'Root of Key (Bypass AI)':
  - Bypasses all AI and scale selection.
  - Plays strictly the fundamental root note of key '${state.detectedKey}' on every beat (quarter note).
  - Use this to verify that the key signature matches the track and downbeat alignment is sample-accurate.

• 'Root & 3rd Alternator (Bypass AI)':
  - Bypasses AI heuristics.
  - Alternates strictly between the active chord's root note (on the beat) and its minor or major 3rd (on the offbeat) as eighth notes.
  - Adapts dynamically to chord quality (minor 3rd for minor chords, major 3rd for major chords).
  - Use this to verify harmonic adaptation and 8th-note rhythmic subdivision.
        """.trimIndent()
    }

    private fun handleAnalysisResult(result: AudioAnalyzer.AnalysisResult, isDownbeat: Boolean) {
        val currentTrack = _uiState.value.activeTrack
        val isPresetOrFile = currentTrack.sourceType == BackingSourceType.PRESET || currentTrack.sourceType == BackingSourceType.LOCAL_FILE
        val stableBpm = if (isPresetOrFile) currentTrack.bpm else result.estimatedBpm

        _uiState.update { current ->
            current.copy(
                detectedPitchHz = result.pitchHz,
                detectedNoteName = result.noteName,
                detectedCents = result.cents,
                detectedConfidence = result.confidence,
                detectedBpm = stableBpm,
                beatPulse = if (isPresetOrFile) isDownbeat else result.isBeatOnset,
                detectedKey = if (isPresetOrFile) currentTrack.defaultKey else result.detectedKey,
                detectedScale = if (isPresetOrFile) currentTrack.defaultScale else result.detectedScale
            )
        }

        // Keep improviser locked to the rock-solid musical tempo
        improviser.currentBpm = stableBpm
        improviser.currentKey = _uiState.value.detectedKey
        improviser.currentScale = _uiState.value.detectedScale
    }

    fun toggleJam() {
        val currentState = _uiState.value.isJamming
        if (currentState) {
            stopJam()
        } else {
            startJam()
        }
    }

    fun startJam() {
        synth.start()
        synth.isSoloMuted = _uiState.value.isSoloMuted
        improviser.clearLookaheadBuffer()
        localNotesCache.clear()
        cloudNotesCache.clear()
        lastPrefetchedBar = -1

        // Pre-populate page 0 (bars 0 & 1) and prefetch page 1 (bars 2 & 3)
        prefetchBars(listOf(0, 1, 2, 3))

        backingPlayer.start()
        improviser.activeMusician = _uiState.value.activeMusician
        improviser.currentBpm = _uiState.value.activeTrack.bpm
        improviser.currentKey = _uiState.value.activeTrack.defaultKey
        improviser.currentScale = _uiState.value.activeTrack.defaultScale
        improviser.isPaused = _uiState.value.isSoloMuted
        improviser.start()

        _uiState.update {
            it.copy(
                isJamming = true,
                currentPlayingBar = 0,
                currentPlayingSub = 0,
                batchedBarRange = Pair(1, 2)
            )
        }
    }

    fun stopJam() {
        improviser.stop()
        backingPlayer.stop()
        synth.stop()
        improviser.clearLookaheadBuffer()
        lastPrefetchedBar = -1
        _uiState.update { it.copy(isJamming = false) }
    }

    fun selectMusician(musician: MusicianProfile) {
        improviser.activeMusician = musician
        synth.activeInstrument = musician.defaultInstrument
        _uiState.update {
            it.copy(
                activeMusician = musician,
                activeInstrument = musician.defaultInstrument
            )
        }
    }

    fun selectInstrument(instrument: LeadInstrument) {
        synth.activeInstrument = instrument
        _uiState.update { it.copy(activeInstrument = instrument) }
    }

    fun selectTrack(track: BackingTrack) {
        val wasPlaying = _uiState.value.isJamming
        if (wasPlaying) {
            stopJam()
        }
        backingPlayer.selectTrack(track)
        val recommendedMusician = MusicianRegistry.getById(track.recommendedMusicianId)
        selectMusician(recommendedMusician)

        _uiState.update {
            it.copy(
                activeTrack = track,
                detectedKey = track.defaultKey,
                detectedScale = track.defaultScale,
                detectedBpm = track.bpm,
                timeSignature = track.timeSignature,
                beatsPerBar = track.beatsPerBar
            )
        }

        if (wasPlaying) {
            startJam()
        }
        refreshCloudBatch()
    }

    fun loadLocalAudioFile(uri: Uri, fileName: String) {
        val track = backingPlayer.loadLocalAudio(uri, fileName)
        selectTrack(track)
        startJam()
    }

    fun startMicRecording(): Boolean {
        stopJam()
        val success = backingPlayer.startMicRecording()
        if (success) {
            _uiState.update { it.copy(isMicRecording = true) }
        }
        return success
    }

    fun stopMicRecording() {
        val loopedTrack = backingPlayer.stopMicRecordingAndLoop()
        _uiState.update { it.copy(isMicRecording = false) }
        if (loopedTrack != null) {
            selectTrack(loopedTrack)
            startJam()
        }
    }

    fun setSoloDensity(density: Float) {
        improviser.densityMultiplier = density
        _uiState.update { it.copy(soloDensity = density) }
    }

    fun setCreativity(creativity: Float) {
        improviser.creativity = creativity
        _uiState.update { it.copy(creativity = creativity) }
    }

    fun setDrive(drive: Float) {
        synth.driveAmount = drive
        _uiState.update { it.copy(driveAmount = drive) }
    }

    fun setFilterCutoff(cutoff: Float) {
        synth.filterCutoff = cutoff
        _uiState.update { it.copy(filterCutoff = cutoff) }
    }

    fun setVibrato(vib: Float) {
        synth.vibratoAmount = vib
        _uiState.update { it.copy(vibratoAmount = vib) }
    }

    fun setMasterVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _uiState.update { it.copy(masterVolume = clamped) }
        applyMixerBalances()
    }

    fun setBackingVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _uiState.update { it.copy(backingVolume = clamped) }
        applyMixerBalances()
    }

    fun setSoloVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _uiState.update { it.copy(soloVolume = clamped) }
        applyMixerBalances()
    }

    fun setCrossfaderBalance(balance: Float) {
        val clamped = balance.coerceIn(0f, 1f)
        _uiState.update { it.copy(crossfaderBalance = clamped) }
        applyMixerBalances()
    }

    fun toggleSoloMute() {
        val newMute = !_uiState.value.isSoloMuted
        _uiState.update { it.copy(isSoloMuted = newMute) }
        if (newMute) {
            synth.allNotesOff()
        }
        applyMixerBalances()
    }

    fun toggleBackingMute() {
        val newMute = !_uiState.value.isBackingMuted
        _uiState.update { it.copy(isBackingMuted = newMute) }
        applyMixerBalances()
    }

    private fun applyMixerBalances() {
        val state = _uiState.value
        // Equal power/linear crossfader calculation:
        // When balance <= 0.5: backing is 100%, solo scales from 0 to 100%
        // When balance >= 0.5: solo is 100%, backing scales from 100% down to 0%
        val crossBacking = if (state.crossfaderBalance <= 0.5f) 1.0f else (1.0f - state.crossfaderBalance) * 2.0f
        val crossSolo = if (state.crossfaderBalance >= 0.5f) 1.0f else state.crossfaderBalance * 2.0f

        val effectiveBacking = (state.backingVolume * crossBacking * state.masterVolume).coerceIn(0f, 1f)
        val effectiveSolo = (state.soloVolume * crossSolo).coerceIn(0f, 1f)

        backingPlayer.volume = effectiveBacking
        backingPlayer.isMuted = state.isBackingMuted

        synth.masterVolume = state.masterVolume
        synth.soloVolume = effectiveSolo
        synth.isSoloMuted = state.isSoloMuted
        improviser.isPaused = state.isSoloMuted
    }

    private fun prefetchBars(barIndices: List<Int>) {
        if (barIndices.isEmpty()) return
        val maxBar = barIndices.maxOrNull() ?: return
        if (lastPrefetchedBar >= maxBar) return
        lastPrefetchedBar = maxBar

        val state = _uiState.value
        val track = state.activeTrack
        val upcomingBars = barIndices.map { b ->
            Pair(b, backingPlayer.getChordContextForBar(track, b))
        }

        // 1. Local Algorithmic Generation - instant with zero latency
        val localBatch = improviser.generateLocalBatch(
            upcomingBars = upcomingBars,
            musician = state.activeMusician,
            key = state.detectedKey,
            scale = state.detectedScale,
            densityMultiplier = state.soloDensity,
            creativity = state.creativity,
            octaveShift = state.octaveShift
        )
        for ((b, _) in upcomingBars) {
            localNotesCache[b] = localBatch.filter { it.barIndex == b }
        }

        // Keep localBatchedNotes updated for the current active page
        val pageStartBar = (_uiState.value.currentPlayingBar / 2) * 2
        val currentLocal = (localNotesCache[pageStartBar] ?: emptyList()) + (localNotesCache[pageStartBar + 1] ?: emptyList())
        _uiState.update { it.copy(localBatchedNotes = currentLocal) }

        // 2. Cloud AI Batch Generation (Gemini SSE Streaming, OpenAI, Claude)
        val provider = state.activeLlmProvider
        val targetCloudProvider = if (provider != LlmProvider.LOCAL_THEORY) provider else LlmProvider.GEMINI
        val apiKey = when (targetCloudProvider) {
            LlmProvider.GEMINI -> state.geminiApiKey
            LlmProvider.OPENAI -> state.openAiApiKey
            LlmProvider.CLAUDE -> state.claudeApiKey
            LlmProvider.LOCAL_THEORY -> ""
        }

        if (apiKey.isBlank()) {
            val aiPreview = cloudLlmClient.generateAiPhrasePreview(
                musician = state.activeMusician,
                key = state.detectedKey,
                upcomingBars = upcomingBars,
                octaveShift = state.octaveShift,
                customDirective = state.customAiPromptDirective
            )
            for ((b, _) in upcomingBars) {
                cloudNotesCache[b] = aiPreview.filter { it.barIndex == b }
            }
            improviser.enqueueLookaheadNotes(aiPreview)
            val count = improviser.getBufferedNoteCount()
            val currentCloud = (cloudNotesCache[pageStartBar] ?: emptyList()) + (cloudNotesCache[pageStartBar + 1] ?: emptyList())
            _uiState.update {
                it.copy(
                    isLlmFetching = false,
                    cloudBatchedNotes = currentCloud,
                    bufferedNoteCount = count,
                    llmStatusMessage = if (provider != LlmProvider.LOCAL_THEORY)
                        "⚡ AI Solo Model ($count notes queued) • Add key in ⚙️ for live Gemini"
                    else
                        "Local Theory Active (AI Solo Ready)"
                )
            }
            return
        }

        _uiState.update { it.copy(isLlmFetching = true) }

        viewModelScope.launch(Dispatchers.IO) {
            val result = cloudLlmClient.generateLookaheadNotes(
                provider = targetCloudProvider,
                apiKey = apiKey,
                musician = state.activeMusician,
                bpm = state.detectedBpm,
                key = state.detectedKey,
                upcomingBars = upcomingBars,
                customDirective = state.customAiPromptDirective,
                useFamousPieceMode = state.useFamousPiecePromptMode,
                timeSignature = state.timeSignature,
                beatsPerBar = state.beatsPerBar,
                onStreamPartialNotes = { partialNotes ->
                    for ((b, _) in upcomingBars) {
                        val bNotes = partialNotes.filter { it.barIndex == b }
                        if (bNotes.isNotEmpty()) {
                            cloudNotesCache[b] = bNotes
                        }
                    }
                    improviser.enqueueLookaheadNotes(partialNotes)
                    val curPage = (_uiState.value.currentPlayingBar / 2) * 2
                    val liveCloud = (cloudNotesCache[curPage] ?: emptyList()) + (cloudNotesCache[curPage + 1] ?: emptyList())
                    _uiState.update { it.copy(cloudBatchedNotes = liveCloud) }
                }
            )

            result.onSuccess { genResult ->
                for ((b, _) in upcomingBars) {
                    cloudNotesCache[b] = genResult.notes.filter { it.barIndex == b }
                }
                improviser.enqueueLookaheadNotes(genResult.notes)
                val count = improviser.getBufferedNoteCount()
                val curPage = (_uiState.value.currentPlayingBar / 2) * 2
                val currentCloud = (cloudNotesCache[curPage] ?: emptyList()) + (cloudNotesCache[curPage + 1] ?: emptyList())
                val resolvedModel = genResult.providerModel.ifBlank { cloudLlmClient.getResolvedGeminiModel() }
                _uiState.update {
                    it.copy(
                        isLlmFetching = false,
                        cloudBatchedNotes = currentCloud,
                        bufferedNoteCount = count,
                        activeResolvedModel = resolvedModel,
                        llmStatusMessage = "🟢 ${targetCloudProvider.displayName} ($resolvedModel): $count notes (${genResult.latencyMs}ms)"
                    )
                }
            }.onFailure { err ->
                val fallbackNotes = cloudLlmClient.generateAiPhrasePreview(
                    musician = state.activeMusician,
                    key = state.detectedKey,
                    upcomingBars = upcomingBars,
                    octaveShift = state.octaveShift,
                    customDirective = state.customAiPromptDirective
                )
                for ((b, _) in upcomingBars) {
                    cloudNotesCache[b] = fallbackNotes.filter { it.barIndex == b }
                }
                improviser.enqueueLookaheadNotes(fallbackNotes)
                val count = improviser.getBufferedNoteCount()
                val curPage = (_uiState.value.currentPlayingBar / 2) * 2
                val currentCloud = (cloudNotesCache[curPage] ?: emptyList()) + (cloudNotesCache[curPage + 1] ?: emptyList())
                _uiState.update {
                    it.copy(
                        isLlmFetching = false,
                        cloudBatchedNotes = currentCloud,
                        bufferedNoteCount = count,
                        llmStatusMessage = "⚡ ${targetCloudProvider.displayName} (AI Solo Model Ready)"
                    )
                }
            }
        }
    }

    fun updateCustomAiPrompt(directive: String) {
        val clean = directive.trim()
        prefs.edit().putString("custom_ai_prompt", clean).apply()
        _uiState.update { it.copy(customAiPromptDirective = clean) }
        refreshCloudBatch()
    }

    fun getCompiledPromptPreview(): String {
        val state = _uiState.value
        val track = state.activeTrack
        val pageStart = (state.currentPlayingBar / 2) * 2
        val nextBar1 = pageStart
        val nextBar2 = pageStart + 1
        val chord1 = backingPlayer.getChordContextForBar(track, nextBar1)
        val chord2 = backingPlayer.getChordContextForBar(track, nextBar2)
        val upcomingBars = listOf(Pair(nextBar1, chord1), Pair(nextBar2, chord2))
        return cloudLlmClient.buildPrompt(
            musician = state.activeMusician,
            bpm = state.detectedBpm,
            key = state.detectedKey,
            upcomingBars = upcomingBars,
            customDirective = state.customAiPromptDirective,
            useFamousPieceMode = state.useFamousPiecePromptMode,
            timeSignature = state.timeSignature,
            beatsPerBar = state.beatsPerBar
        )
    }

    fun toggleFamousPiecePrompt(enabled: Boolean) {
        prefs.edit().putBoolean("use_famous_piece_prompt", enabled).apply()
        _uiState.update {
            it.copy(
                useFamousPiecePromptMode = enabled,
                llmStatusMessage = if (enabled)
                    "Song Repertoire Prompt: '${it.activeMusician.famousPiece}' active"
                else
                    "Analytical Engine Prompt: Density & swing parameters active"
            )
        }
        refreshCloudBatch()
    }

    fun toggleLocalBackup(enabled: Boolean) {
        prefs.edit().putBoolean("local_backup_enabled", enabled).apply()
        improviser.isLocalBackupEnabled = enabled
        _uiState.update {
            it.copy(
                isLocalBackupEnabled = enabled,
                llmStatusMessage = if (enabled) "Local engine backup enabled for Cloud AI" else "Local engine backup disabled (pure Cloud AI audio)"
            )
        }
    }

    fun setAudioOutputDestination(destination: com.example.ai.AudioOutputDestination) {
        val isCloud = destination == com.example.ai.AudioOutputDestination.CLOUD
        improviser.audioOutputSource = if (isCloud) Improviser.AudioOutputSource.CLOUD else Improviser.AudioOutputSource.LOCAL
        improviser.isCloudLookaheadEnabled = isCloud
        _uiState.update {
            it.copy(
                audioOutputDestination = destination,
                llmStatusMessage = if (isCloud) "Audio routed to Cloud AI (${it.activeLlmProvider.displayName})" else "Audio routed to Local Theory Engine"
            )
        }
    }

    fun toggleNotationExpanded() {
        _uiState.update { it.copy(isNotationExpanded = !it.isNotationExpanded) }
    }

    fun toggleMixerExpanded() {
        _uiState.update { it.copy(isMixerExpanded = !it.isMixerExpanded) }
    }

    fun refreshCloudBatch() {
        val curBar = _uiState.value.currentPlayingBar
        val pageStart = (curBar / 2) * 2
        cloudNotesCache.remove(pageStart)
        cloudNotesCache.remove(pageStart + 1)
        cloudNotesCache.remove(pageStart + 2)
        cloudNotesCache.remove(pageStart + 3)
        lastPrefetchedBar = -1
        prefetchBars(listOf(pageStart, pageStart + 1, pageStart + 2, pageStart + 3))
    }

    fun setLlmProvider(provider: LlmProvider) {
        prefs.edit().putString("llm_provider", provider.name).apply()
        val dest = if (provider == LlmProvider.LOCAL_THEORY)
            com.example.ai.AudioOutputDestination.LOCAL
        else
            com.example.ai.AudioOutputDestination.CLOUD
        setAudioOutputDestination(dest)
        _uiState.update {
            it.copy(
                activeLlmProvider = provider,
                llmStatusMessage = if (provider == LlmProvider.LOCAL_THEORY) "Local Theory Engine Active" else "${provider.displayName} Selected"
            )
        }
        if (provider != LlmProvider.LOCAL_THEORY) {
            refreshCloudBatch()
        }
    }

    fun saveApiKey(provider: LlmProvider, key: String) {
        val clean = key.trim()
        when (provider) {
            LlmProvider.GEMINI -> {
                prefs.edit().putString("gemini_key", clean).apply()
                _uiState.update { it.copy(geminiApiKey = clean) }
            }
            LlmProvider.OPENAI -> {
                prefs.edit().putString("openai_key", clean).apply()
                _uiState.update { it.copy(openAiApiKey = clean) }
            }
            LlmProvider.CLAUDE -> {
                prefs.edit().putString("claude_key", clean).apply()
                _uiState.update { it.copy(claudeApiKey = clean) }
            }
            else -> {}
        }
    }

    fun toggleLookaheadBatching(enabled: Boolean) {
        prefs.edit().putBoolean("lookahead_enabled", enabled).apply()
        val isCloud = enabled && _uiState.value.activeLlmProvider != LlmProvider.LOCAL_THEORY
        improviser.isCloudLookaheadEnabled = isCloud
        if (!isCloud) {
            improviser.clearLookaheadBuffer()
            lastPrefetchedBar = -1
        }
        _uiState.update {
            it.copy(
                isLookaheadBatchingEnabled = enabled,
                bufferedNoteCount = if (isCloud) it.bufferedNoteCount else 0,
                llmStatusMessage = if (isCloud) "Lookahead Batching Active (${it.activeLlmProvider.displayName})" else "Local Theory Engine Active"
            )
        }
    }

    fun testLlmConnection(provider: LlmProvider, overrideKey: String = "", onResult: (Boolean, String) -> Unit) {
        val key = if (overrideKey.isNotBlank()) {
            overrideKey.trim()
        } else {
            when (provider) {
                LlmProvider.GEMINI -> _uiState.value.geminiApiKey
                LlmProvider.OPENAI -> _uiState.value.openAiApiKey
                LlmProvider.CLAUDE -> _uiState.value.claudeApiKey
                LlmProvider.LOCAL_THEORY -> ""
            }
        }
        viewModelScope.launch {
            val res = cloudLlmClient.testConnection(provider, key)
            res.onSuccess { msg ->
                _uiState.update { it.copy(activeResolvedModel = cloudLlmClient.getResolvedGeminiModel()) }
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.message ?: "Connection failed")
            }
        }
    }

    fun setPitchBend(semitones: Float) {
        synth.pitchBendSemitones = semitones
    }

    fun triggerSoloNote(midiNote: Int) {
        synth.noteOn(midiNote, 0.85f)
        viewModelScope.launch {
            delay(200)
            synth.noteOff(midiNote)
        }
    }

    fun setKeyAndScale(key: String, scale: ScaleType) {
        improviser.currentKey = key
        improviser.currentScale = scale
        _uiState.update {
            it.copy(
                detectedKey = key,
                detectedScale = scale
            )
        }
    }

    fun openComparisonMatrixDialog() {
        _uiState.update { it.copy(showComparisonMatrixDialog = true) }
    }

    fun closeComparisonMatrixDialog() {
        _uiState.update { it.copy(showComparisonMatrixDialog = false) }
    }

    fun closeComparisonGalleryDialog() {
        stopAudition()
        _uiState.update { it.copy(showComparisonGalleryDialog = false) }
    }

    fun launchComparisonSession(session: ComparisonSession) {
        val state = _uiState.value
        val track = state.activeTrack

        // Compute chord context for all bars in the session
        val chords = (0 until session.barCount).map { b ->
            Pair(b, backingPlayer.getChordContextForBar(track, b))
        }
        val fullSession = session.copy(
            trackStyle = track.artistOrStyle,
            chords = chords
        )

        // Seed initial empty results
        val initialResults = fullSession.variants.map { v ->
            ComparisonVariantResult(
                variant = v,
                musician = MusicianRegistry.getById(v.musicianId),
                isGenerating = true,
                statusMessage = "Queued..."
            )
        }

        _uiState.update {
            it.copy(
                showComparisonMatrixDialog = false,
                showComparisonGalleryDialog = true,
                activeComparisonSession = fullSession,
                comparisonResults = initialResults,
                isComparisonGenerating = true
            )
        }

        val provider = state.activeLlmProvider
        val apiKey = when (provider) {
            LlmProvider.GEMINI -> state.geminiApiKey
            LlmProvider.OPENAI -> state.openAiApiKey
            LlmProvider.CLAUDE -> state.claudeApiKey
            LlmProvider.LOCAL_THEORY -> ""
        }

        viewModelScope.launch(Dispatchers.IO) {
            comparisonBatchEngine.runComparisonBatch(
                session = fullSession,
                provider = provider,
                apiKey = apiKey,
                onVariantCompleted = { completedVariant ->
                    _uiState.update { current ->
                        val updated = current.comparisonResults.map { old ->
                            if (old.variant.id == completedVariant.variant.id) completedVariant else old
                        }
                        val stillGenerating = updated.any { it.isGenerating }
                        current.copy(
                            comparisonResults = updated,
                            isComparisonGenerating = stillGenerating
                        )
                    }
                }
            )
        }
    }

    fun reRollComparisonVariant(variantId: String) {
        val session = _uiState.value.activeComparisonSession ?: return
        val variant = session.variants.find { it.id == variantId } ?: return
        val state = _uiState.value

        _uiState.update { current ->
            val updated = current.comparisonResults.map {
                if (it.variant.id == variantId) it.copy(isGenerating = true, statusMessage = "Regenerating...") else it
            }
            current.copy(comparisonResults = updated, isComparisonGenerating = true)
        }

        val provider = state.activeLlmProvider
        val apiKey = when (provider) {
            LlmProvider.GEMINI -> state.geminiApiKey
            LlmProvider.OPENAI -> state.openAiApiKey
            LlmProvider.CLAUDE -> state.claudeApiKey
            LlmProvider.LOCAL_THEORY -> ""
        }

        viewModelScope.launch(Dispatchers.IO) {
            val singleSession = session.copy(variants = listOf(variant))
            comparisonBatchEngine.runComparisonBatch(
                session = singleSession,
                provider = provider,
                apiKey = apiKey,
                onVariantCompleted = { completed ->
                    _uiState.update { current ->
                        val updated = current.comparisonResults.map {
                            if (it.variant.id == variantId) completed else it
                        }
                        current.copy(
                            comparisonResults = updated,
                            isComparisonGenerating = updated.any { it.isGenerating }
                        )
                    }
                }
            )
        }
    }

    fun auditionVariant(notes: List<LookaheadNote>, bpm: Int, beatsPerBar: Int) {
        stopAudition()
        if (notes.isEmpty()) return

        val state = _uiState.value
        val variantId = state.comparisonResults.find { it.notes == notes }?.variant?.id

        _uiState.update {
            it.copy(
                isAuditioning = true,
                auditionVariantId = variantId
            )
        }

        auditionJob = viewModelScope.launch(Dispatchers.Default) {
            synth.start()
            val safeBpm = bpm.coerceIn(40, 300)
            val subDurationMs = (60_000L / safeBpm) / 4L // 16th note subdivision duration

            // Sort notes by barIndex and subInBar
            val sortedNotes = notes.sortedWith(compareBy({ it.barIndex }, { it.subInBar }))
            val maxBar = sortedNotes.maxOfOrNull { it.barIndex } ?: 0
            val totalSubs = (maxBar + 1) * (beatsPerBar * 4)

            var currentGlobalSub = 0
            while (currentGlobalSub < totalSubs && isActive) {
                val currentBar = currentGlobalSub / (beatsPerBar * 4)
                val currentSubInBar = currentGlobalSub % (beatsPerBar * 4)

                val startingNotes = sortedNotes.filter { it.barIndex == currentBar && it.subInBar == currentSubInBar }
                for (n in startingNotes) {
                    synth.noteOn(n.midiPitch, n.velocity)
                    // Schedule noteOff after note.durationSubdivisions
                    launch {
                        delay(n.durationSubdivisions * subDurationMs)
                        synth.noteOff(n.midiPitch)
                    }
                }

                delay(subDurationMs)
                currentGlobalSub++
            }

            // Cleanup when playback completes
            synth.allNotesOff()
            _uiState.update {
                it.copy(
                    isAuditioning = false,
                    auditionVariantId = null
                )
            }
        }
    }

    fun stopAudition() {
        auditionJob?.cancel()
        auditionJob = null
        synth.allNotesOff()
        _uiState.update {
            it.copy(
                isAuditioning = false,
                auditionVariantId = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudition()
        stopJam()
    }
}
