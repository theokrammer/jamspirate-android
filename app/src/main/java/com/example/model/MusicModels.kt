package com.example.model

import androidx.compose.ui.graphics.Color

enum class ScaleType(val displayName: String, val intervals: IntArray) {
    DORIAN("Dorian (Jazz/Modal)", intArrayOf(0, 2, 3, 5, 7, 9, 10)),
    BLUES("Blues Scale", intArrayOf(0, 3, 5, 6, 7, 10)),
    MINOR_PENTATONIC("Minor Pentatonic", intArrayOf(0, 3, 5, 7, 10)),
    MAJOR_PENTATONIC("Major Pentatonic", intArrayOf(0, 2, 4, 7, 9)),
    MIOLYDIAN("Mixolydian (Rock/Funk)", intArrayOf(0, 2, 4, 5, 7, 9, 10)),
    HARMONIC_MINOR("Harmonic Minor (Neo-Classical)", intArrayOf(0, 2, 3, 5, 7, 8, 11)),
    PHRYGIAN_DOMINANT("Phrygian Dominant (Flamenco)", intArrayOf(0, 1, 4, 5, 7, 8, 10)),
    AEOLIAN("Natural Minor", intArrayOf(0, 2, 3, 5, 7, 8, 10)),
    MAJOR("Major (Ionian)", intArrayOf(0, 2, 4, 5, 7, 9, 11)),
    BEBOP_DOMINANT("Bebop Dominant", intArrayOf(0, 2, 4, 5, 7, 9, 10, 11))
}

enum class LeadInstrument(
    val id: String,
    val displayName: String,
    val category: String,
    val description: String,
    val accentColor: Color
) {
    PRODIGY_RAVE_SYNTH(
        id = "prodigy_synth",
        displayName = "Prodigy Rave Lead",
        category = "Electronic / Rave",
        description = "Aggressive detuned dual-saw hook with resonant filter bite and sub-bass grit",
        accentColor = Color(0xFF84CC16) // Acid green
    ),
    HIGH_GAIN_GUITAR(
        id = "high_gain_guitar",
        displayName = "High-Gain Metal",
        category = "Electric Guitar",
        description = "Heavy diode saturation, 4x12 cabinet resonance, screaming sustained harmonics",
        accentColor = Color(0xFFEF4444) // Fiery red
    ),
    OVERDRIVE_GUITAR(
        id = "overdrive_guitar",
        displayName = "Warm Overdrive",
        category = "Electric Guitar",
        description = "Dynamic tube breakup, singing touch sensitivity, and harmonic bloom",
        accentColor = Color(0xFFF59E0B) // Amber
    ),
    TENOR_SAX(
        id = "tenor_sax",
        displayName = "Tenor Saxophone",
        category = "Wind & Brass",
        description = "Warm breathy attack, formant-shaped body, and expressive vibrato",
        accentColor = Color(0xFFEAB308) // Brass gold
    ),
    FLAMENCO_GUITAR(
        id = "flamenco_nylon",
        displayName = "Flamenco Nylon",
        category = "Acoustic Guitar",
        description = "Crisp fingernail picado attack and wooden soundboard body resonance",
        accentColor = Color(0xFFF97316) // Spanish orange
    ),
    JAZZ_TRUMPET(
        id = "jazz_trumpet",
        displayName = "Jazz Harmon Trumpet",
        category = "Wind & Brass",
        description = "Crisp brass attack filtered through a classic vintage harmon mute",
        accentColor = Color(0xFF06B6D4) // Cyan
    ),
    RHODES_PIANO(
        id = "rhodes_piano",
        displayName = "Fender Rhodes",
        category = "Electric Keys",
        description = "Bell-like metallic chime attack with warm chorused stereo decay",
        accentColor = Color(0xFF8B5CF6) // Purple
    ),
    GRAND_PIANO(
        id = "grand_piano",
        displayName = "Concert Grand",
        category = "Acoustic Keys",
        description = "Harmonic hammer transient, dynamic touch velocity, and rich acoustic decay",
        accentColor = Color(0xFF3B82F6) // Blue
    )
}

enum class MusicianCategory {
    JAZZ, ROCK_BLUES, VIRTUOSO_SHRED, FLAMENCO, CLASSICAL, DIAGNOSTIC
}

data class MusicianProfile(
    val id: String,
    val name: String,
    val eraOrGenre: String,
    val category: MusicianCategory,
    val signatureStyle: String,
    val famousPiece: String = "",
    val preferredScales: List<ScaleType>,
    val defaultInstrument: LeadInstrument,
    val noteDensity: Float,       // 0.1 (sparse/patient) to 1.0 (blazing cascades)
    val swingFactor: Float,       // 0.0 (straight) to 0.7 (heavy swing)
    val restProbability: Float,   // Probability of pausing to breathe
    val bendTendency: Float,      // Probability of pitch bends/slides
    val arpeggioTendency: Float,  // Probability of scalar vs arpeggiated movement
    val outsideNoteTendency: Float, // Tension/chromatic passing tones
    val accentColor: Color
)

object MusicianRegistry {
    val ALL_MUSICIANS: List<MusicianProfile> = listOf(
        MusicianProfile(
            id = "dave_brubeck",
            name = "Dave Brubeck",
            eraOrGenre = "Cool Jazz & 5/4 Swing",
            category = MusicianCategory.JAZZ,
            signatureStyle = "Asymmetrical time phrasing, syncopated polyrhythms, and playful West Coast blues motifs.",
            famousPiece = "Take Five",
            preferredScales = listOf(ScaleType.DORIAN, ScaleType.BLUES, ScaleType.BEBOP_DOMINANT),
            defaultInstrument = LeadInstrument.RHODES_PIANO,
            noteDensity = 0.58f,
            swingFactor = 0.55f,
            restProbability = 0.28f,
            bendTendency = 0.12f,
            arpeggioTendency = 0.65f,
            outsideNoteTendency = 0.22f,
            accentColor = Color(0xFF06B6D4)
        ),
        MusicianProfile(
            id = "miles_davis",
            name = "Miles Davis",
            eraOrGenre = "Modal & Cool Jazz",
            category = MusicianCategory.JAZZ,
            signatureStyle = "Minimalist space, dramatic pauses, lyrical color tones (9ths, 11ths), and muted horn warmth.",
            famousPiece = "So What",
            preferredScales = listOf(ScaleType.DORIAN, ScaleType.BLUES, ScaleType.MINOR_PENTATONIC),
            defaultInstrument = LeadInstrument.JAZZ_TRUMPET,
            noteDensity = 0.32f,
            swingFactor = 0.60f,
            restProbability = 0.50f,
            bendTendency = 0.40f,
            arpeggioTendency = 0.25f,
            outsideNoteTendency = 0.30f,
            accentColor = Color(0xFF38BDF8)
        ),
        MusicianProfile(
            id = "john_coltrane",
            name = "John Coltrane",
            eraOrGenre = "Hard Bop & Free Jazz",
            category = MusicianCategory.JAZZ,
            signatureStyle = "\"Sheets of sound\", rapid scalar cascades, complex harmonic substitutions, and intense energy.",
            famousPiece = "Giant Steps",
            preferredScales = listOf(ScaleType.BEBOP_DOMINANT, ScaleType.DORIAN, ScaleType.HARMONIC_MINOR),
            defaultInstrument = LeadInstrument.TENOR_SAX,
            noteDensity = 0.92f,
            swingFactor = 0.45f,
            restProbability = 0.12f,
            bendTendency = 0.25f,
            arpeggioTendency = 0.70f,
            outsideNoteTendency = 0.42f,
            accentColor = Color(0xFFFBBF24)
        ),
        MusicianProfile(
            id = "eric_johnson",
            name = "Eric Johnson",
            eraOrGenre = "Melodic Fusion & Shred",
            category = MusicianCategory.VIRTUOSO_SHRED,
            signatureStyle = "Cascading pentatonic waterfalls, violin-like singing sustain, and open-interval triad arpeggios.",
            famousPiece = "Cliffs of Dover",
            preferredScales = listOf(ScaleType.MAJOR_PENTATONIC, ScaleType.MINOR_PENTATONIC, ScaleType.DORIAN),
            defaultInstrument = LeadInstrument.OVERDRIVE_GUITAR,
            noteDensity = 0.78f,
            swingFactor = 0.15f,
            restProbability = 0.22f,
            bendTendency = 0.45f,
            arpeggioTendency = 0.68f,
            outsideNoteTendency = 0.14f,
            accentColor = Color(0xFFA855F7)
        ),
        MusicianProfile(
            id = "paco_de_lucia",
            name = "Paco de Lucía",
            eraOrGenre = "Modern Flamenco Virtuoso",
            category = MusicianCategory.FLAMENCO,
            signatureStyle = "Lightning-fast picado runs, Spanish Phrygian dominant tensions, and sharp syncopated dynamic bursts.",
            famousPiece = "Entre dos aguas",
            preferredScales = listOf(ScaleType.PHRYGIAN_DOMINANT, ScaleType.HARMONIC_MINOR, ScaleType.AEOLIAN),
            defaultInstrument = LeadInstrument.FLAMENCO_GUITAR,
            noteDensity = 0.88f,
            swingFactor = 0.20f,
            restProbability = 0.18f,
            bendTendency = 0.20f,
            arpeggioTendency = 0.72f,
            outsideNoteTendency = 0.25f,
            accentColor = Color(0xFFF97316)
        ),
        MusicianProfile(
            id = "eddie_van_halen",
            name = "Eddie Van Halen",
            eraOrGenre = "Hard Rock & Metal",
            category = MusicianCategory.VIRTUOSO_SHRED,
            signatureStyle = "Two-handed tapped arpeggios, aggressive trill bursts, wide blues vibrato, and stadium fire.",
            famousPiece = "Eruption",
            preferredScales = listOf(ScaleType.MINOR_PENTATONIC, ScaleType.BLUES, ScaleType.MIOLYDIAN),
            defaultInstrument = LeadInstrument.HIGH_GAIN_GUITAR,
            noteDensity = 0.85f,
            swingFactor = 0.30f,
            restProbability = 0.20f,
            bendTendency = 0.65f,
            arpeggioTendency = 0.80f,
            outsideNoteTendency = 0.20f,
            accentColor = Color(0xFFEF4444)
        ),
        MusicianProfile(
            id = "slash",
            name = "Slash",
            eraOrGenre = "Hard Rock & Blues-Rock",
            category = MusicianCategory.ROCK_BLUES,
            signatureStyle = "Soulful vocal bends, melodic minor/harmonic hooks, greasy pentatonic runs, and singing sustained notes.",
            famousPiece = "Sweet Child O' Mine",
            preferredScales = listOf(ScaleType.BLUES, ScaleType.MINOR_PENTATONIC, ScaleType.DORIAN, ScaleType.HARMONIC_MINOR),
            defaultInstrument = LeadInstrument.HIGH_GAIN_GUITAR,
            noteDensity = 0.62f,
            swingFactor = 0.35f,
            restProbability = 0.30f,
            bendTendency = 0.75f,
            arpeggioTendency = 0.35f,
            outsideNoteTendency = 0.15f,
            accentColor = Color(0xFFDC2626)
        ),
        MusicianProfile(
            id = "randy_rhoads",
            name = "Randy Rhoads",
            eraOrGenre = "Neo-Classical Heavy Metal",
            category = MusicianCategory.VIRTUOSO_SHRED,
            signatureStyle = "Classical minor scale sweeps, diminished 7th arpeggios, precision pick articulation, and dramatic pedal-tones.",
            famousPiece = "Crazy Train",
            preferredScales = listOf(ScaleType.HARMONIC_MINOR, ScaleType.AEOLIAN, ScaleType.MINOR_PENTATONIC),
            defaultInstrument = LeadInstrument.HIGH_GAIN_GUITAR,
            noteDensity = 0.84f,
            swingFactor = 0.05f,
            restProbability = 0.18f,
            bendTendency = 0.50f,
            arpeggioTendency = 0.75f,
            outsideNoteTendency = 0.22f,
            accentColor = Color(0xFFE11D48)
        ),
        MusicianProfile(
            id = "mark_knopfler",
            name = "Mark Knopfler",
            eraOrGenre = "Roots Rock & Fingerstyle",
            category = MusicianCategory.ROCK_BLUES,
            signatureStyle = "Expressive fingerpicked tone, melodic staccato punctuation, warm double-stops, and storytelling phrasing.",
            famousPiece = "Sultans of Swing",
            preferredScales = listOf(ScaleType.DORIAN, ScaleType.MINOR_PENTATONIC, ScaleType.MAJOR_PENTATONIC),
            defaultInstrument = LeadInstrument.OVERDRIVE_GUITAR,
            noteDensity = 0.52f,
            swingFactor = 0.40f,
            restProbability = 0.35f,
            bendTendency = 0.55f,
            arpeggioTendency = 0.45f,
            outsideNoteTendency = 0.10f,
            accentColor = Color(0xFF10B981)
        ),
        MusicianProfile(
            id = "jimi_hendrix",
            name = "Jimi Hendrix",
            eraOrGenre = "Psychedelic Blues-Rock",
            category = MusicianCategory.ROCK_BLUES,
            signatureStyle = "Fuzz-drenched wild bends, major/minor pentatonic ambiguity, aggressive double-stops, and rhythmic swagger.",
            famousPiece = "Voodoo Child (Slight Return)",
            preferredScales = listOf(ScaleType.BLUES, ScaleType.MINOR_PENTATONIC, ScaleType.MIOLYDIAN),
            defaultInstrument = LeadInstrument.HIGH_GAIN_GUITAR,
            noteDensity = 0.68f,
            swingFactor = 0.45f,
            restProbability = 0.28f,
            bendTendency = 0.80f,
            arpeggioTendency = 0.38f,
            outsideNoteTendency = 0.28f,
            accentColor = Color(0xFF8B5CF6)
        ),
        MusicianProfile(
            id = "david_gilmour",
            name = "David Gilmour",
            eraOrGenre = "Progressive & Melodic Rock",
            category = MusicianCategory.ROCK_BLUES,
            signatureStyle = "Majestic soaring sustain, delayed emotional resolutions, rich blues vibrato, and pristine melodic hooks.",
            famousPiece = "Comfortably Numb",
            preferredScales = listOf(ScaleType.MINOR_PENTATONIC, ScaleType.DORIAN, ScaleType.BLUES),
            defaultInstrument = LeadInstrument.OVERDRIVE_GUITAR,
            noteDensity = 0.40f,
            swingFactor = 0.20f,
            restProbability = 0.45f,
            bendTendency = 0.85f,
            arpeggioTendency = 0.25f,
            outsideNoteTendency = 0.08f,
            accentColor = Color(0xFF22D3EE)
        ),
        MusicianProfile(
            id = "js_bach",
            name = "J.S. Bach",
            eraOrGenre = "Baroque Master",
            category = MusicianCategory.CLASSICAL,
            signatureStyle = "Rigorous counterpoint logic, symmetrical circle-of-fifths sequences, inverted motifs, and perpetual scalar motion.",
            famousPiece = "Toccata and Fugue in D minor",
            preferredScales = listOf(ScaleType.HARMONIC_MINOR, ScaleType.MAJOR, ScaleType.AEOLIAN),
            defaultInstrument = LeadInstrument.GRAND_PIANO,
            noteDensity = 0.80f,
            swingFactor = 0.00f,
            restProbability = 0.10f,
            bendTendency = 0.02f,
            arpeggioTendency = 0.85f,
            outsideNoteTendency = 0.18f,
            accentColor = Color(0xFFE2E8F0)
        ),
        MusicianProfile(
            id = "beethoven",
            name = "Ludwig van Beethoven",
            eraOrGenre = "Romantic Classical Titan",
            category = MusicianCategory.CLASSICAL,
            signatureStyle = "Explosive dynamic contrasts (pp to ff), tragic minor-key arpeggiation, and relentless 4-note motif development.",
            famousPiece = "Symphony No. 5 in C minor",
            preferredScales = listOf(ScaleType.HARMONIC_MINOR, ScaleType.AEOLIAN, ScaleType.MAJOR),
            defaultInstrument = LeadInstrument.GRAND_PIANO,
            noteDensity = 0.72f,
            swingFactor = 0.00f,
            restProbability = 0.24f,
            bendTendency = 0.05f,
            arpeggioTendency = 0.78f,
            outsideNoteTendency = 0.20f,
            accentColor = Color(0xFFF59E0B)
        ),
        MusicianProfile(
            id = "diagnostic_root",
            name = "Root of Key (Bypass AI)",
            eraOrGenre = "Bypass AI - Fundamental Key Root",
            category = MusicianCategory.DIAGNOSTIC,
            signatureStyle = "Bypasses AI generation. Plays strictly the root note of the current key on every quarter beat. Used to determine if poor sound is due to harmonic drift or timing jitter.",
            famousPiece = "Root Anchor Cadence",
            preferredScales = listOf(ScaleType.MAJOR, ScaleType.AEOLIAN),
            defaultInstrument = LeadInstrument.OVERDRIVE_GUITAR,
            noteDensity = 0.50f,
            swingFactor = 0.00f,
            restProbability = 0.00f,
            bendTendency = 0.00f,
            arpeggioTendency = 0.00f,
            outsideNoteTendency = 0.00f,
            accentColor = Color(0xFF10B981) // Diagnostic Emerald
        ),
        MusicianProfile(
            id = "diagnostic_root_third",
            name = "Root & 3rd Alternator (Bypass AI)",
            eraOrGenre = "Bypass AI - Chord 3rd Eighths",
            category = MusicianCategory.DIAGNOSTIC,
            signatureStyle = "Bypasses AI. Plays alternating eighth notes: chord root on the beat, and minor or major 3rd on the offbeat, dynamically adapting to each backing chord.",
            famousPiece = "Chord 3rd Harmony Etude",
            preferredScales = listOf(ScaleType.MAJOR, ScaleType.DORIAN),
            defaultInstrument = LeadInstrument.HIGH_GAIN_GUITAR,
            noteDensity = 0.80f,
            swingFactor = 0.00f,
            restProbability = 0.00f,
            bendTendency = 0.00f,
            arpeggioTendency = 1.00f,
            outsideNoteTendency = 0.00f,
            accentColor = Color(0xFF06B6D4) // Diagnostic Cyan
        )
    )

    fun getById(id: String): MusicianProfile {
        return ALL_MUSICIANS.find { it.id == id }
            ?: (ALL_MUSICIANS.find { it.id == "eric_johnson" } ?: ALL_MUSICIANS.first())
    }
}

data class ChordContext(
    val rootPitchClass: Int,
    val isMinor: Boolean,
    val chordName: String
)

data class JamNoteEvent(
    val id: Long,
    val midiNote: Int,
    val noteName: String,
    val frequencyHz: Float,
    val startTimeMs: Long,
    val durationMs: Long,
    val velocity: Float,
    val isBend: Boolean = false,
    val musicianName: String
)

enum class BackingSourceType {
    PRESET, LOCAL_FILE, MIC_LOOP
}

data class BackingTrack(
    val id: String,
    val title: String,
    val artistOrStyle: String,
    val bpm: Int,
    val defaultKey: String,
    val defaultScale: ScaleType,
    val recommendedMusicianId: String,
    val sourceType: BackingSourceType,
    val durationSec: Int = 120,
    val audioUri: String? = null,
    val timeSignature: String = "4/4",
    val beatsPerBar: Int = 4
)
