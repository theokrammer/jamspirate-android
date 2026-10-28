# Jamspirate! - Real-Time AI Music Improvisation Studio

Jamspirate! is a real-time musical improvisation companion and live soloing engine for Android. It listens to backing tracks (loaded from MIDI, MP3, OGG, or captured live via microphone), detects pitch, key, chord centers, and tempo in real time, and synthesizes expressive musical solos influenced by legendary musicians across jazz, rock, metal, electronic, flamenco, and classical music.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The instrument sound engine has been expanded to include a crushing **High-Gain Guitar** and an **Aggressive Rave Synth** inspired by the gritty, overdriven hook in The Prodigy's *"Invaders Must Die"*:

- **Confirmed Mode - Autonomous AI Soloist**: The system autonomously improvises real-time musical solos that harmonically and rhythmically lock into the active backing track.
- **Confirmed Influences - 13 Master Musicians**:
  - *Dave Brubeck*: Asymmetrical time phrasing (5/4 and 9/8 syncopation), block-chord polyrhythms, and playful West Coast blues-jazz melodies.
  - *Miles Davis*: Modal jazz, spacious phrasing, muted horn expression, long sustained color tones (9ths, 11ths), and dramatic tension pauses.
  - *John Coltrane*: "Sheets of sound", rapid scalar cascades, pentatonic substitutions, high energy density, and harmonic extensions.
  - *Eric Johnson*: Pentatonic waterfalls, violin-like singing tone, cascading open-triad intervals, and pristine melodic voice-leading.
  - *Paco de Lucía*: Lightning-fast picado scalar runs, Flamenco Phrygian / Phrygian Dominant modes, and sharp syncopated dynamic bursts.
  - *Eddie Van Halen*: Blazing two-handed tapped arpeggios, aggressive trills, wide blues vibrato, and electric arena energy.
  - *Slash*: Emotional vocal-like bends, Dorian and Blues hybrid runs, sustained melodic hooks, and hard-rock grit.
  - *Randy Rhoads*: Neo-classical metal fusion, Aeolian and Harmonic Minor scale sequences, diminished arpeggios, and precise classical articulation.
  - *Mark Knopfler*: Subtle fingerstyle phrasing, lyrical staccato licks, tasteful bends, and dynamic storytelling melodies.
  - *Jimi Hendrix*: Emotive blues-rock bends, major/minor pentatonic hybrids, double-stops, and wild vibrato.
  - *David Gilmour*: Soaring sustained notes, lyrical slow blues melodies, delayed harmonic resolutions, and epic tone.
  - *J.S. Bach*: Baroque counterpoint motifs, circle-of-fifths sequences, tight arpeggiated logic, and inverted thematic lines.
  - *Ludwig van Beethoven*: Dramatic dynamic shifts (pianissimo to fortissimo), passionate minor-key arpeggios, and rhythmic motif development.
- **Expanded Lead Voice Palette - Multi-Genre Synthesis Engine**:
  - *Aggressive Rave Synth (Prodigy "Invaders Must Die" Style)*: Dual detuned hard-driven saw waves, aggressive resonant low-pass filter bite, punchy sub-oscillator, and saturated bit-edge grit.
  - *High-Gain Guitar*: Heavy distortion with asymmetric clipping diode simulation, speaker cabinet frequency filtering (mid-punch, low-end thump, steep high-cut), and screaming sustained harmonics.
  - *Clean / Warm Overdrive Guitar*: Touch-sensitive breakup, pick transient, string resonance, and dynamic bloom.
  - *Tenor Saxophone*: Warm breathy attack, formant filtering, vibrato, and dynamic reed expression.
  - *Flamenco / Nylon Guitar*: Crisp fingernail transient, wooden soundboard body resonance, fast decay.
  - *Muted/Jazz Trumpet*: Crisp brassy bite with harmon mute formant filtering.
  - *Fender Rhodes*: Bell-like tine transient, warm chorused electric piano decay.
  - *Grand Piano*: Rich multi-harmonic acoustic decay and hammer dynamics.
- **Backing Track Pipeline**:
  - Preloaded curated jam tracks across Jazz, Blues, Rock, Metal, Electronic, Flamenco, and Classical.
  - Multi-format file importer supporting MIDI (`.mid`), MP3, OGG, and WAV.
  - Real-time Live Looper / Microphone Recorder with automatic loop quantization, pitch profiling, and BPM locking.

---

### 1. Overview & Core Concept

- **What It Does**: Jamspirate! turns your Android device into a responsive, virtuoso jam companion. Feed it any backing track or record a groove live, and Jamspirate! analyzes harmonic pitch, musical key centers, and rhythm in real time, improvising dynamic solos in the distinct playing signature of legendary musicians.
- **Target Audience / Persona**: Guitarists, electronic producers, pianists, horn players, and students seeking inspiring real-time accompaniment and deep stylistic exploration.
- **Key Value**: Delivers real-time interactive soloing that breathes, pauses, accents, and phrases like real musical masters instead of static randomized MIDI generators.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Studio Deck & Transport**: The user enters the main Studio console with dual decks: Backing Track Deck (left/top) and Improvisation Solo Deck (right/bottom).
2. **Backing Track Selection**:
   - Choose from built-in jam tracks (e.g., *Take Five Jazz Groove*, *12-Bar Chicago Blues*, *Invaders Electronic Cyber-Beat*, *Heavy Metal Shred Jam*, *Comfortably Numb Ballad*, *Flamenco Bulerías*, *Beethoven Moonlight Motif*).
   - Or tap **Import Audio/MIDI** to pick any local file.
   - Or tap **Record Live Mic** with countdown and live waveform monitor to record a guitar, keyboard, or vocal groove.
3. **Musician Influence & Instrument Selection**:
   - Horizontal sliding carousel or categorical grid of 13 legendary musicians with visual portrait badges, genre tags, and breakdown cards (phrasing density, swing factor, scale preference, trademark technique).
   - Instrument voice selector featuring the new **High-Gain Guitar** and **Aggressive Prodigy Synth**, alongside Saxophone, Overdrive Guitar, Flamenco Nylon, Jazz Trumpet, Rhodes, and Grand Piano.
   - Real-time expressive control ribbon for manual pitch bend, modulation, or solo nudge.
4. **Live Jamming & Real-Time Visualization**:
   - Tap **Start Jam**: backing track starts, the Real-Time Pitch & Beat Analyzer kicks in with glowing oscilloscope and neon audio visualizer.
   - Live Piano Roll displays detected chord/notes versus improvised solo notes in real time.
   - Live controls: Solo Density, Harmonic Adventure (Safe / Outside), Volume Balance, and Drive/Filter Cutoff.

#### Visual Identity & Theme
- **Aesthetic Direction**: High-end analog-digital hybrid studio console. Dark obsidian brushed aluminum backdrop with glowing neon status indicators, vintage VU meters, and luminous oscilloscope graphs.
- **Color Palette**:
  - Primary Background: Deep Slate `#0B0F17` / `#111827`
  - Studio Rack Surface: Midnight Slate `#1A2234`
  - Neon Cyan Accent (Audio & Analysis): `#06B6D4` / `#22D3EE`
  - Electric Amber (Warm Tube / Solo Instrument): `#F59E0B` / `#FBBF24`
  - Glowing Magenta (Musician Phrasing & Pitch Track): `#EC4899`
  - Aggressive Acid Green / Orange (Prodigy Synth & High-Gain Accents): `#84CC16` / `#F97316`
- **Typography**:
  - Technical monospaced readouts for BPM, key, note pitch, and latency.
  - Modern geometric sans-serif for responsive usability.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Modular Phrasing Grammar for 13 Distinct Artists**
  - *Chosen Approach*: Implement a structured `MusicianProfile` registry where each artist defines their probability curves for note length (e.g. 16th-note bursts for Van Halen/Coltrane vs 4-beat sustained bends for Gilmour/Miles), scale weights (Bebop, Blues, Phrygian Dominant, Harmonic Minor, Pentatonic), ornament styles (tapping, hammer-on/pull-off, picado, vibrato, staccato), and pause distributions.
  - *Why*: Allows instant stylistic switching during live playback without interrupting audio streaming.
- **Decision 2: Pure Kotlin Low-Latency PCM Audio Synthesis (`AudioTrack`)**
  - *Chosen Approach*: Stream synthesized 44.1 kHz 16-bit PCM directly to an `AudioTrack` instance on an audio priority thread with customizable DSP algorithms for High-Gain waveshaping, Prodigy-style detuned saw waves with resonant lowpass filter sweeps, and acoustic instruments.
  - *Why*: Ensures immediate response, custom multi-timbral synthesis without external binary dependencies.
- **Decision 3: Dual Audio Source Manager**
  - *Chosen Approach*: Unified backing audio engine supporting Android `MediaPlayer` for compressed audio, custom parser for MIDI files, and `AudioRecord` for live microphone capture.
  - *Why*: Seamlessly satisfies all requested input media formats.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jamspirate! Studio UI                           │
│  ┌───────────────────────┐  ┌───────────────────────────────────────┐  │
│  │ Backing Track Deck    │  │ Improvisation Solo Deck               │  │
│  │ - Presets / File Pick │  │ - 13 Musician Profiles (Miles, EVH...)│  │
│  │ - Mic Live Looper     │  │ - Lead Voice (High-Gain, Prodigy Synth│  │
│  │ - Waveform & Transport│  │   Sax, Overdrive Gtr, Nylon, Rhodes..)│  │
│  │                       │  │ - Solo Density, Drive, Cutoff, Tone   │  │
│  └───────────┬───────────┘  └───────────────────┬───────────────────┘  │
│              │                                  │                      │
│  ┌───────────▼──────────────────────────────────▼───────────────────┐  │
│  │ Live Visualizers: Real-Time Pitch Ribbon, Oscilloscope, VU Meters│  │
│  └──────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           StudioViewModel                              │
│   Manages PlaybackState, JamState, DetectedBpm, DetectedPitch, etc.    │
└──────────────┬──────────────────────────────────────────┬──────────────┘
               │                                          │
               ▼                                          ▼
┌──────────────────────────────┐          ┌──────────────────────────────┐
│  Real-Time Audio Analyzer    │          │  AI Improvisation Engine     │
│  - Pitch Detection (YIN/ACF) │─────────▶│  - Music Theory Scale Engine │
│  - Beat & Tempo Detection    │  Chord & │  - 13 Musician Phrasing Model│
│  - Energy & Onset Profiling  │   Beat   │  - Note & Rhythm Generator   │
└──────────────┬───────────────┘          └──────────────┬───────────────┘
               │                                         │
               │                                         │ MIDI/Note Events
               │                                         ▼
┌──────────────▼───────────────┐          ┌──────────────────────────────┐
│  Backing Audio Player        │          │  Real-Time Sound Synthesizer │
│  - Android MediaPlayer (MP3) │          │  - Multi-Timbral SoftSynth   │
│  - MIDI Parser & Player      │          │  - High-Gain & Prodigy DSP   │
│  - Mic AudioRecord Looper    │          │  - Low-Latency AudioTrack    │
└──────────────────────────────┘          └──────────────────────────────┘
```

#### Key Architecture Components & State Mapping
- `AudioAnalyzer`: Continuously processes 2048-sample audio buffers. Calculates fundamental frequency $f_0$, maps to musical pitch, detects key signatures, and detects beat onsets for real-time BPM estimation.
- `MusicianProfile`: Detailed algorithmic models for Dave Brubeck, Miles Davis, John Coltrane, Eric Johnson, Paco de Lucía, Eddie Van Halen, Slash, Randy Rhoads, Mark Knopfler, Jimi Hendrix, David Gilmour, J.S. Bach, and Ludwig van Beethoven.
- `LeadInstrument`: Multi-timbral synthesizer supporting:
  - *Prodigy Rave Synth*: Detuned saw dual-oscillator with resonant filter envelope and saturation.
  - *High-Gain Guitar*: Asymmetrical clipping waveshaping + 4x12 cab frequency contouring.
  - *Overdrive Electric Guitar*: Warm tube saturation and open transients.
  - *Tenor Saxophone*: Formant-filtered saw with expressive reed vibrato.
  - *Flamenco Nylon Guitar*: Crisp nail strike transient and acoustic body resonance.
  - *Jazz Harmon Trumpet*: Crisp brassy bite with formant-filtered harmonic mute.
  - *Fender Rhodes Piano*: FM sine + bell chime transient with chorus decay.
  - *Grand Piano*: Dynamic hammer acoustics and rich decay.
- `BackingTrackManager`: Coordinates preset tracks, local file picker (MIDI, MP3, OGG, WAV), and live mic capture with permission management.
- `StudioViewModel`: High-performance state coordinator publishing live frequencies, pitch trails, active notes, and player states.
