package com.example.comparison

import androidx.compose.ui.graphics.Color
import com.example.ai.LookaheadNote
import com.example.model.ChordContext
import com.example.model.MusicianProfile
import com.example.model.MusicianRegistry

data class ComparisonVariant(
    val id: String,
    val label: String,
    val musicianId: String,
    val customDirective: String = "",
    val useFamousPieceMode: Boolean = false,
    val famousPieceName: String = "",
    val temperature: Float = 0.85f,
    val accentColorArgb: Long = 0xFF38BDF8
) {
    val color: Color get() = Color(accentColorArgb)
}

data class ComparisonPreset(
    val id: String,
    val title: String,
    val description: String,
    val variants: List<ComparisonVariant>
)

object ComparisonPresets {
    fun createDefaultPresets(baseMusicianId: String = "eric_johnson"): List<ComparisonPreset> {
        val baseProfile = MusicianRegistry.getById(baseMusicianId)
        val famousPiece = baseProfile.famousPiece.ifBlank { "Signature Motif" }

        return listOf(
            ComparisonPreset(
                id = "prompt_lab",
                title = "Prompt Lab (Same Influence)",
                description = "Compare 3 distinct stylistic directives under ${baseProfile.name}",
                variants = listOf(
                    ComparisonVariant(
                        id = "var_prompt_1",
                        label = "Cascading Triplets",
                        musicianId = baseMusicianId,
                        customDirective = "Rapid cascading pentatonic triplets with hybrid picking and singing vibrato",
                        accentColorArgb = 0xFF38BDF8 // Sky blue
                    ),
                    ComparisonVariant(
                        id = "var_prompt_2",
                        label = "Chromatic / Outside",
                        musicianId = baseMusicianId,
                        customDirective = "Outside chromatic passing tones, tritone substitutions, and unexpected tension resolutions",
                        accentColorArgb = 0xFFF59E0B // Amber
                    ),
                    ComparisonVariant(
                        id = "var_prompt_3",
                        label = "Lyrical Sustained",
                        musicianId = baseMusicianId,
                        customDirective = "Spacious lyrical singing notes, long sustains, breathing room, and delayed emotional bends",
                        accentColorArgb = 0xFF10B981 // Emerald
                    )
                )
            ),
            ComparisonPreset(
                id = "master_duel",
                title = "Cross-Influence Shootout",
                description = "Coltrane vs Eric Johnson vs David Gilmour vs B.B. King on the exact same chord progression",
                variants = listOf(
                    ComparisonVariant(
                        id = "var_coltrane",
                        label = "John Coltrane",
                        musicianId = "john_coltrane",
                        customDirective = "Rapid sheets of sound, three-tonic superimpositions, and explosive modal density",
                        accentColorArgb = 0xFFEC4899 // Pink
                    ),
                    ComparisonVariant(
                        id = "var_ej",
                        label = "Eric Johnson",
                        musicianId = "eric_johnson",
                        customDirective = "Pristine pentatonic violin cascades, triad inversions, and chord melody hooks",
                        accentColorArgb = 0xFF38BDF8 // Sky Blue
                    ),
                    ComparisonVariant(
                        id = "var_gilmour",
                        label = "David Gilmour",
                        musicianId = "david_gilmour",
                        customDirective = "Majestic blues vibrato, patient delayed resolutions, and singing sustain",
                        accentColorArgb = 0xFF22D3EE // Cyan
                    ),
                    ComparisonVariant(
                        id = "var_bbking",
                        label = "B.B. King",
                        musicianId = "bb_king",
                        customDirective = "The King's box, stinging index vibrato, call-and-response vocal spacing",
                        accentColorArgb = 0xFFF59E0B // Amber
                    )
                )
            ),
            ComparisonPreset(
                id = "famous_piece_vs_ai",
                title = "Famous Piece vs AI Parameters",
                description = "Authentic '$famousPiece' transcription motif vs dynamic AI generative variations",
                variants = listOf(
                    ComparisonVariant(
                        id = "var_ref_famous",
                        label = "Master Piece Motif",
                        musicianId = baseMusicianId,
                        useFamousPieceMode = true,
                        famousPieceName = famousPiece,
                        customDirective = "Faithful rendition of the famous theme and melodic hooks from $famousPiece",
                        accentColorArgb = 0xFFF59E0B // Amber
                    ),
                    ComparisonVariant(
                        id = "var_ai_outside",
                        label = "AI Free Improvisation",
                        musicianId = baseMusicianId,
                        useFamousPieceMode = false,
                        customDirective = "High creativity modern reinterpretation with blues inflections and syncopation",
                        accentColorArgb = 0xFFA855F7 // Purple
                    ),
                    ComparisonVariant(
                        id = "var_ai_minimal",
                        label = "AI Sparse Melodic",
                        musicianId = baseMusicianId,
                        useFamousPieceMode = false,
                        customDirective = "Sparse patient target-tone solo emphasizing chord thirds and fifths",
                        accentColorArgb = 0xFF10B981 // Emerald
                    )
                )
            ),
            ComparisonPreset(
                id = "genre_collision",
                title = "Baroque vs Blues vs Modern",
                description = "J.S. Bach counterpoint vs Jimi Hendrix fuzz bends vs Miles Davis cool space",
                variants = listOf(
                    ComparisonVariant(
                        id = "var_bach",
                        label = "J.S. Bach",
                        musicianId = "js_bach",
                        customDirective = "Baroque arpeggiated circle-of-fifths counterpoint and symmetrical scalar sequences",
                        accentColorArgb = 0xFFE2E8F0 // Silver
                    ),
                    ComparisonVariant(
                        id = "var_hendrix",
                        label = "Jimi Hendrix",
                        musicianId = "jimi_hendrix",
                        customDirective = "Aggressive double-stops, wild half-step blues bends, and rhythmic swagger",
                        accentColorArgb = 0xFFEF4444 // Red
                    ),
                    ComparisonVariant(
                        id = "var_miles",
                        label = "Miles Davis",
                        musicianId = "miles_davis",
                        customDirective = "Cool modal minimalism, harmon mute spacing, and sparse harmonic focus",
                        accentColorArgb = 0xFF06B6D4 // Cyan
                    )
                )
            )
        )
    }
}

data class ComparisonSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val trackId: String,
    val trackTitle: String,
    val trackStyle: String = "",
    val bpm: Int,
    val key: String,
    val timeSignature: String = "4/4",
    val beatsPerBar: Int = 4,
    val barCount: Int = 4, // 4 or 8 bars
    val startBarIndex: Int = 0,
    val chords: List<Pair<Int, ChordContext>> = emptyList(),
    val variants: List<ComparisonVariant> = emptyList()
)

data class ComparisonVariantResult(
    val variant: ComparisonVariant,
    val musician: MusicianProfile,
    val notes: List<LookaheadNote> = emptyList(),
    val latencyMs: Long = 0L,
    val isGenerating: Boolean = false,
    val statusMessage: String = "",
    val error: String? = null
) {
    val noteCount: Int get() = notes.size

    val minMidi: Int get() = notes.minOfOrNull { it.midiPitch } ?: 60
    val maxMidi: Int get() = notes.maxOfOrNull { it.midiPitch } ?: 60

    val pitchRangeSemitones: Int get() = if (notes.isNotEmpty()) (maxMidi - minMidi) else 0

    val averageDensityPerBar: Float
        get() = if (notes.isNotEmpty()) {
            val bars = notes.map { it.barIndex }.distinct().size.coerceAtLeast(1)
            notes.size.toFloat() / bars.toFloat()
        } else 0f
}
