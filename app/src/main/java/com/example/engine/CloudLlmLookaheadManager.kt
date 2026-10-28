package com.example.engine

import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class CloudLlmLookaheadManager(
    private val client: CloudLlmClient = CloudLlmClient(),
    private val scope: CoroutineScope
) {
    private var config: CloudLlmConfig = CloudLlmConfig()

    // Notes mapped by Pair(BarIndex, SubInBar)
    private val lookaheadNotes = ConcurrentHashMap<Pair<Int, Int>, CloudNoteEvent>()
    private val requestedBars = ConcurrentHashMap.newKeySet<Int>()
    private val loadedBars = ConcurrentHashMap.newKeySet<Int>()

    private val _status = MutableStateFlow(CloudLookaheadStatus())
    val status: StateFlow<CloudLookaheadStatus> = _status.asStateFlow()

    private var currentActiveJob: Job? = null

    fun updateConfig(newConfig: CloudLlmConfig) {
        val wasEnabled = config.isEnabled
        config = newConfig
        _status.update {
            it.copy(
                isEnabled = newConfig.isEnabled,
                currentProvider = newConfig.provider,
                activeModel = newConfig.getEffectiveModel()
            )
        }
        if (!newConfig.isEnabled && wasEnabled) {
            clearQueue()
        }
    }

    fun getConfig(): CloudLlmConfig = config

    fun clearQueue() {
        lookaheadNotes.clear()
        requestedBars.clear()
        loadedBars.clear()
        _status.update {
            it.copy(
                bufferedBarsSummary = "Queue cleared",
                lastNoteCount = 0
            )
        }
    }

    fun isBarLoaded(barIndex: Int): Boolean = loadedBars.contains(barIndex)

    fun getNoteForTick(barIndex: Int, subInBar: Int): CloudNoteEvent? {
        return lookaheadNotes[Pair(barIndex, subInBar)]
    }

    fun checkAndTriggerLookahead(
        currentBarIndex: Int,
        track: BackingTrack,
        musician: MusicianProfile,
        bpm: Int,
        key: String,
        scale: ScaleType,
        chordLookup: (targetBar: Int) -> ChordContext
    ) {
        if (!config.isEnabled) return
        val apiKey = config.getApiKeyForCurrentProvider()
        if (apiKey.isBlank()) {
            _status.update { it.copy(lastError = "API key missing for ${config.provider.displayName}") }
            return
        }

        val targetBar = currentBarIndex + 1
        // Prune old bars
        if (currentBarIndex > 4) {
            val cutoff = currentBarIndex - 2
            lookaheadNotes.keys.removeIf { it.first < cutoff }
            requestedBars.removeIf { it < cutoff }
            loadedBars.removeIf { it < cutoff }
        }

        if (requestedBars.contains(targetBar)) {
            return // Already requested or buffered
        }
        requestedBars.add(targetBar)

        val lookaheadBarsCount = config.lookaheadBars.coerceIn(1, 2)
        val upcomingChords = (0 until lookaheadBarsCount).map { offset ->
            val barNum = targetBar + offset
            val chord = chordLookup(barNum)
            "Bar $barNum: ${chord.chordName} (${if (chord.isMinor) "Minor" else "Major"}, root pitch class ${chord.rootPitchClass})"
        }

        val prompt = buildLookaheadPrompt(
            targetBar = targetBar,
            lookaheadBarsCount = lookaheadBarsCount,
            upcomingChords = upcomingChords,
            musician = musician,
            bpm = bpm,
            key = key,
            scale = scale,
            track = track
        )

        _status.update {
            it.copy(
                isRequesting = true,
                bufferedBarsSummary = "Fetching Bar $targetBar...",
                lastPromptSent = prompt
            )
        }

        currentActiveJob = scope.launch(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val result = client.fetchLookaheadBatch(
                provider = config.provider,
                apiKey = apiKey,
                model = config.getEffectiveModel(),
                prompt = prompt
            )
            val duration = System.currentTimeMillis() - startTime

            result.onSuccess { (notes, rawResponse) ->
                for (note in notes) {
                    val actualBar = targetBar + note.barOffset
                    lookaheadNotes[Pair(actualBar, note.subInBar)] = note
                }
                for (b in 0 until lookaheadBarsCount) {
                    loadedBars.add(targetBar + b)
                }

                _status.update { current ->
                    current.copy(
                        isRequesting = false,
                        lastLatencyMs = duration,
                        lastNoteCount = notes.size,
                        bufferedBarsSummary = "Buffered Bar $targetBar (+${notes.size} notes in ${duration}ms)",
                        lastError = null,
                        lastRawResponse = rawResponse
                    )
                }
            }.onFailure { error ->
                _status.update { current ->
                    current.copy(
                        isRequesting = false,
                        lastLatencyMs = duration,
                        lastError = error.message,
                        bufferedBarsSummary = "Fallback to Local Engine (Request failed)"
                    )
                }
            }
        }
    }

    private fun buildLookaheadPrompt(
        targetBar: Int,
        lookaheadBarsCount: Int,
        upcomingChords: List<String>,
        musician: MusicianProfile,
        bpm: Int,
        key: String,
        scale: ScaleType,
        track: BackingTrack
    ): String {
        return """
You are an elite lead musical improviser channeling ${musician.name} (${musician.category}).
Generate a $lookaheadBarsCount-bar musical solo line in response to the upcoming chord changes.

TRACK INFO:
- Title: ${track.title} (${track.artistOrStyle})
- Tempo: $bpm BPM (1 bar = 4 beats = 16 sixteenth-note subdivisions, indices 0..15)
- Overall Key: $key ${scale.displayName}

UPCOMING CHORDS TO SOLO OVER:
${upcomingChords.joinToString("\n")}

MUSICIAN STYLE PROFILE:
- Style: ${musician.signatureStyle}
- Density: ${musician.noteDensity} (higher = more notes/arpeggios, lower = spacious)
- Arpeggio Tendency: ${musician.arpeggioTendency}
- Bends/Slides: ${musician.bendTendency}

OUTPUT FORMAT:
Respond with ONLY a raw JSON array of note objects. No markdown formatting, no explanations.
Each object must have:
- "barOffset": 0 (for first bar, Bar $targetBar) or 1 (for second bar, if present)
- "subInBar": integer from 0 to 15 (sixteenth-note subdivision within the bar: 0=beat1, 4=beat2, 8=beat3, 12=beat4)
- "midiNote": integer MIDI pitch (e.g. 57=A3, 60=C4, 69=A4, 72=C5, range 45..88)
- "noteName": note name string (e.g. "C4", "E4", "G4", "A4")
- "durationBeats": duration in beats (e.g. 0.25=16th, 0.5=8th, 1.0=quarter)
- "velocity": dynamic volume float from 0.4 to 1.0
- "isBend": boolean

EXAMPLE OUTPUT:
[
  {"barOffset": 0, "subInBar": 0, "midiNote": 60, "noteName": "C4", "durationBeats": 0.5, "velocity": 0.88, "isBend": false},
  {"barOffset": 0, "subInBar": 4, "midiNote": 64, "noteName": "E4", "durationBeats": 0.5, "velocity": 0.82, "isBend": false},
  {"barOffset": 0, "subInBar": 8, "midiNote": 67, "noteName": "G4", "durationBeats": 1.0, "velocity": 0.90, "isBend": false}
]
""".trimIndent()
    }
}
