# Implementation Plan: Sheet Music Comparison Matrix & PDF Exporter

## Summary
Enable musicians and composers to rapidly compare multiple AI improvisation approaches over the same backing track bars without waiting for real-time playback. Users configure a 2-4 variant matrix (contrasting prompts, influences, and reference piece transcriptions), generate multi-bar improvisations via background batch lookahead, inspect them in an in-app side-by-side or stacked sheet music gallery with synchronized bar zoom and audio audition, and export/print high-resolution multi-page PDF sheet music with chord charts, clefs, and comparative annotations.

---

## User Review & Critical Decisions

### 1. Matrix Configuration & Presets
- **Matrix Setup**: Users can select 2 to 4 variants to compare simultaneously.
- **Comparison Modes**:
  1. *Prompt Variations with Same Influence* (e.g., "Arpeggiated blues licks" vs "Outside modal chromaticism" both under *Eric Johnson*).
  2. *Cross-Influence Shootout* (e.g., *John Coltrane* vs *Miles Davis* vs *Wes Montgomery* on the same 8-bar chord progression).
  3. *Famous Piece / Master Transcriptions vs AI Parameters* (e.g., Authentic *Take Five* Paul Desmond motif vs AI Desmond with custom temperature & syncopation).
- **Preset Quick-Launch**: 1-tap quick presets ("Master Duel", "Style Contrast", "Prompt Lab") to eliminate manual setup friction.

### 2. Fast Background Batch Generation
- **Zero Playback Waiting**: The app calculates the chord sequence for 4, 8, or 16 bars ahead and dispatches parallel/sequential lookahead batch requests directly to the Gemini API (`batchGenerateMeasures`).
- **Progressive Stave Rendering**: As each variant returns, staves populate immediately with smooth shimmer/reveal animations.
- **Offline Fallback**: If network or Gemini quota limits occur, local deterministic voice-leading engine seeds candidate bars so the user is never left with empty staves.

### 3. In-App Interactive Gallery
- **Synchronized Multi-Staff View**: Stacked staves (Variant A, Variant B, Variant C) vertically aligned by measure line, or single-variant focused tab view with rapid swipe switcher.
- **Playhead Audition**: Tap any measure on any variant to audition that specific solo line over the synthesized chord backing track without changing the active song position.
- **Diff & Analysis Overlay**: Visual color highlights indicating note density, dissonance against chord tones, rhythmic syncopation, and scalar roots.

### 4. Printable PDF Sheet Music Export
- **Native Android Vector PDF**: Uses `android.graphics.pdf.PdfDocument` with `Canvas` vector rendering at 300 DPI for crisp vector notes, stems, clefs, key signatures, and measure numbers.
- **Layout Options**:
  - *Score Format (Stacked)*: All variants printed together system-by-system for direct bar-by-bar visual comparison on paper.
  - *Individual Lead Sheets*: Each variant formatted as a clean, standalone lead sheet with track title, influence tag, and prompt subtitle.
- **Direct System Print & Share**: Integrated with Android `PrintManager` (direct WiFi print) and Android system Share Sheet (Save to Google Drive, send via email, or export to PDF reader).

---

## Proposed Changes

### Domain & State Layer
#### `app/src/main/java/com/example/aistudio/comparison/`
- **`ComparisonConfig.kt`**:
  - `ComparisonVariant(id, label, influenceId, promptOverride, temperature, referencePieceName)`
  - `ComparisonSession(id, trackId, keySignature, timeSignature, measureCount, chords, variants)`
  - `ComparisonResult(variantId, generatedBars: List<GeneratedMeasure>, latencyMs, tokenCount)`
- **`ComparisonBatchEngine.kt`**:
  - Coordinates coroutine jobs for multi-variant generation.
  - Builds optimized prompt payloads utilizing the track's harmonic progression.
  - Translates batch responses into `ScoreMeasure` models compatible with existing staff renderers.

### UI & Presentation Layer
#### `app/src/main/java/com/example/aistudio/ui/comparison/`
- **`ComparisonMatrixDialog.kt`**:
  - Configuration sheet to choose 2-4 slots, track excerpt length (4, 8, or 16 bars), and select prompt/artist combinations.
  - Quick-preset chips for instantaneous testing.
- **`ComparisonGalleryScreen.kt`**:
  - Stacked Score View (vertically aligned systems with synchronized horizontal panning).
  - Tabbed Lead Sheet View (single variant enlarged with quick toggle chips).
  - Solo Audition Bar: play button on each variant with mute/solo controls.
  - Top action bar with "Export PDF", "Print", and "Re-Roll Variant" buttons.
- **`ComparativeStaffView.kt`**:
  - Multi-line Vector Canvas rendering measures side-by-side with chord symbols above the staves.
  - Visual badge labels (e.g., "[A] Coltrane - 'Sheets of Sound'", "[B] Miles - 'Modal Space'").

### Export & Printing Layer
#### `app/src/main/java/com/example/aistudio/export/`
- **`SheetMusicPdfExporter.kt`**:
  - Uses `android.graphics.pdf.PdfDocument`.
  - Computes standard A4 / US Letter margins (72 points/inch), header metadata (Song Title, Key, Tempo, Time Signature, Date, Variant Prompts).
  - Draws vector music notation (G-clef, key accidentals, time signature, bar lines, measure numbers, noteheads, stems, beams, flags, accidentals, chord text).
  - Writes to app cache directory and serves via `androidx.core.content.FileProvider`.
  - Invokes `PrintManager` with custom `PrintDocumentAdapter` for 1-tap print dialog.

### Navigation & Top-Level Integration
#### `app/src/main/java/com/example/aistudio/ui/`
- **`StudioScreen.kt` & `MusicNotationView.kt`**:
  - Add "Compare Improvs" action button next to the Sheet Music toggle.
  - Connect `StudioViewModel` to trigger the `ComparisonMatrixDialog`.

---

## Technical Architecture & Flow

```
+-------------------------------------------------------------+
|                  Comparison Matrix Setup                    |
|  Select 2-4 Variants:                                       |
|  [Slot A: Coltrane / Heavy Modal]                           |
|  [Slot B: Eric Johnson / Pentatonic Cascades]               |
|  [Slot C: Reference Piece: Take Five Paul Desmond Motif]    |
|  Excerpt: 8 Measures | Key: Bb Minor | Time: 5/4            |
+------------------------------+------------------------------+
                               | "Run Batch Compare"
                               v
+-------------------------------------------------------------+
|                 ComparisonBatchEngine                       |
|  - Concurrently queries CloudLlmClient (LookaheadBatch)     |
|  - Generates 8 measures for each slot in background coroutine|
|  - Ingests reference piece motifs for direct side-by-side   |
+------------------------------+------------------------------+
                               | Emits ComparisonSession
                               v
+-------------------------------------------------------------+
|                 In-App Comparison Gallery                   |
|  [Stacked Multi-Staff] <---> [Single Swipable Lead Sheet]   |
|  Measure 1 | Measure 2 | Measure 3 | Measure 4 ...          |
|  Slot A: 𝄞 𝅘𝅥𝅯 𝅘𝅥𝅮 𝅘𝅥 ...                                   |
|  Slot B: 𝄞 𝅗 𝅘𝅥𝅮 𝅘𝅥 ...                                     |
|  Slot C: 𝄞 𝅘𝅥 𝅘𝅥 𝅘𝅥 ...                                     |
|  Audition individual lines with backing synth               |
+------------------------------+------------------------------+
                               | "Print / Export PDF"
                               v
+-------------------------------------------------------------+
|                 SheetMusicPdfExporter                       |
|  - android.graphics.pdf.PdfDocument                         |
|  - High-res vector drawing (300 DPI, A4/Letter pagination)  |
|  - Android PrintManager (WiFi Direct Print)                 |
|  - FileProvider + Intent.ACTION_SEND (Share PDF)            |
+-------------------------------------------------------------+
```

---

## Verification Plan

### Automated Unit Tests
- `ComparisonBatchEngineTest.kt`: Verify multi-variant request payload creation, error resilience if one slot fails, and parser validation.
- `SheetMusicPdfExporterTest.kt`: Verify PDF document generation, correct page count calculation, and non-empty file creation.
- Run tests via `gradle :app:testDebugUnitTest`.

### Functional & UI Verification
- Verify matrix setup dialog displays proper influence selections and prompt input fields.
- Verify batch execution without stopping or altering active backing track playback.
- Inspect in-app multi-staff rendering on standard phones and wider aspect ratios.
- Test PDF generation and verify exported file opens in standard Android PDF viewers with accurate clefs, accidentals, and lyrics/chord annotations.
