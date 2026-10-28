package com.example.audio

import kotlin.math.*

/**
 * High-Fidelity Wavetable SoundFont Engine for Jamspirate.
 * Provides studio-quality single-cycle wavetables, attack transients, and
 * acoustic drum soundfont models for backing tracks and lead improvisation.
 */
object WavetableSoundfont {
    const val WAVETABLE_SIZE = 2048
    const val SAMPLE_RATE = 44100

    // Single-cycle band-limited wavetables
    val grandPianoTable = FloatArray(WAVETABLE_SIZE)
    val rhodesTable = FloatArray(WAVETABLE_SIZE)
    val overdriveGuitarTable = FloatArray(WAVETABLE_SIZE)
    val highGainGuitarTable = FloatArray(WAVETABLE_SIZE)
    val tenorSaxTable = FloatArray(WAVETABLE_SIZE)
    val jazzTrumpetTable = FloatArray(WAVETABLE_SIZE)
    val flamencoGuitarTable = FloatArray(WAVETABLE_SIZE)
    val raveSynthTable = FloatArray(WAVETABLE_SIZE)
    val electricBassTable = FloatArray(WAVETABLE_SIZE)
    val uprightBassTable = FloatArray(WAVETABLE_SIZE)

    init {
        generateWavetables()
    }

    private fun generateWavetables() {
        val twoPi = 2.0 * PI

        for (i in 0 until WAVETABLE_SIZE) {
            val phase = (i.toDouble() / WAVETABLE_SIZE) * twoPi

            // 1. Concert Grand Piano: Rich acoustic string harmonics with warm 1st-4th dominance
            var piano = sin(phase) * 0.70 +
                    sin(phase * 2.0) * 0.38 +
                    sin(phase * 3.0) * 0.22 +
                    sin(phase * 4.0) * 0.14 +
                    sin(phase * 5.0) * 0.08 +
                    sin(phase * 6.0) * 0.04
            grandPianoTable[i] = piano.toFloat()

            // 2. Fender Rhodes: Metallic chime tine transient + warm electric bell body (strong 4th & 7th overtone)
            var rhodes = sin(phase) * 0.75 +
                    sin(phase * 2.0) * 0.15 +
                    sin(phase * 4.0) * 0.35 +
                    sin(phase * 7.0) * 0.18 +
                    sin(phase * 9.0) * 0.06
            rhodesTable[i] = rhodes.toFloat()

            // 3. Overdrive Guitar: Warm singing tube breakup (even & odd harmonics)
            var od = sin(phase) * 0.65 +
                    sin(phase * 2.0) * 0.35 +
                    sin(phase * 3.0) * 0.25 +
                    sin(phase * 4.0) * 0.12 +
                    sin(phase * 5.0) * 0.08
            // Gentle saturation
            od = tanh(od * 1.5)
            overdriveGuitarTable[i] = od.toFloat()

            // 4. High-Gain Guitar: Saturated humbucker with Celestion 4x12 cab midrange resonance
            var saw = 2.0 * (i.toDouble() / WAVETABLE_SIZE) - 1.0
            var hg = saw * 0.55 +
                    sin(phase) * 0.35 +
                    sin(phase * 2.0) * 0.30 +
                    sin(phase * 3.0) * 0.22 +
                    sin(phase * 5.0) * 0.15
            hg = if (hg > 0) tanh(hg * 2.2) else -tanh(-hg * 1.8)
            highGainGuitarTable[i] = hg.toFloat()

            // 5. Tenor Saxophone: Reedy acoustic bore with prominent odd harmonics (1st, 3rd, 5th) + breath formant
            var sax = sin(phase) * 0.55 +
                    sin(phase * 2.0) * 0.18 +
                    sin(phase * 3.0) * 0.38 +
                    sin(phase * 4.0) * 0.12 +
                    sin(phase * 5.0) * 0.24 +
                    sin(phase * 7.0) * 0.10
            tenorSaxTable[i] = sax.toFloat()

            // 6. Jazz Trumpet: Harmon muted brass with piercing 2kHz formant boost (strong 3rd, 5th, 6th)
            var tr = sin(phase) * 0.40 +
                    sin(phase * 2.0) * 0.25 +
                    sin(phase * 3.0) * 0.38 +
                    sin(phase * 4.0) * 0.30 +
                    sin(phase * 5.0) * 0.25 +
                    sin(phase * 6.0) * 0.18
            jazzTrumpetTable[i] = tr.toFloat()

            // 7. Flamenco Nylon Guitar: Crisp fingernail attack + cedar wood soundboard resonance
            var nyl = sin(phase) * 0.60 +
                    sin(phase * 2.0) * 0.42 +
                    sin(phase * 3.0) * 0.28 +
                    sin(phase * 4.0) * 0.16 +
                    sin(phase * 5.0) * 0.09
            flamencoGuitarTable[i] = nyl.toFloat()

            // 8. Rave Synth (The Prodigy style): Hypersaw with detuned harmonics and sub-bass weight
            var raveSaw1 = 2.0 * (i.toDouble() / WAVETABLE_SIZE) - 1.0
            var raveSaw2 = 2.0 * (((i * 2) % WAVETABLE_SIZE).toDouble() / WAVETABLE_SIZE) - 1.0
            var rave = raveSaw1 * 0.65 + raveSaw2 * 0.25 + sin(phase) * 0.30
            raveSynthTable[i] = tanh(rave * 1.4).toFloat()

            // 9. Electric Finger Bass: Fender Jazz Bass with punchy fundamental, 2nd harmonic, and fret growl
            var ebass = sin(phase) * 0.82 +
                    sin(phase * 2.0) * 0.45 +
                    sin(phase * 3.0) * 0.20 +
                    sin(phase * 4.0) * 0.08
            ebass = tanh(ebass * 1.25)
            electricBassTable[i] = ebass.toFloat()

            // 10. Upright Acoustic Bass: Warm deep woody resonance, low-order harmonics
            var ubass = sin(phase) * 0.90 +
                    sin(phase * 2.0) * 0.28 +
                    sin(phase * 3.0) * 0.12
            uprightBassTable[i] = ubass.toFloat()
        }

        // Normalize all wavetables to [-1.0, 1.0] range
        normalizeTable(grandPianoTable)
        normalizeTable(rhodesTable)
        normalizeTable(overdriveGuitarTable)
        normalizeTable(highGainGuitarTable)
        normalizeTable(tenorSaxTable)
        normalizeTable(jazzTrumpetTable)
        normalizeTable(flamencoGuitarTable)
        normalizeTable(raveSynthTable)
        normalizeTable(electricBassTable)
        normalizeTable(uprightBassTable)
    }

    private fun normalizeTable(table: FloatArray) {
        var maxPeak = 0.001f
        for (v in table) {
            val a = abs(v)
            if (a > maxPeak) maxPeak = a
        }
        val scale = 0.95f / maxPeak
        for (i in table.indices) {
            table[i] *= scale
        }
    }

    /**
     * Reads a sample from a wavetable with linear interpolation.
     * [phase] is normalized in [0.0, 2*PI).
     */
    inline fun readWavetable(table: FloatArray, phase: Double): Float {
        val pos = (phase / (2.0 * PI) * WAVETABLE_SIZE)
        val idx = pos.toInt() % WAVETABLE_SIZE
        val nextIdx = (idx + 1) % WAVETABLE_SIZE
        val frac = (pos - pos.toInt()).toFloat()
        return table[idx] * (1f - frac) + table[nextIdx] * frac
    }

    /**
     * Fast pseudo-random noise generator for drum & instrument transients.
     */
    class FastRng(var seed: Long = 987654321L) {
        inline fun nextNoise(): Float {
            seed = (seed * 1664525L + 1013904223L) and 0xFFFFFFFFL
            return ((seed ushr 16) and 0xFFFFL).toFloat() / 32768.0f - 1.0f
        }
    }

    /**
     * SoundFont Acoustic Drum Voice.
     * Capable of rendering Kick, Snare, Rimshot, Closed Hat, Open Hat, Ride, Crash, Tom, and Cajon.
     */
    class SoundfontDrumVoice {
        var active = false
        var type = 0 // 1=Kick, 2=Snare, 3=ClosedHat, 4=OpenHat, 5=Ride, 6=Crash, 7=Rimshot, 8=Tom, 9=CajonThud, 10=CajonSlap
        var ageSamples = 0
        var decaySamples = 0
        var phase = 0.0
        var phase2 = 0.0
        var velocity = 0.8f
        var rng = FastRng()
        // Lowpass/bandpass filter state for snares/cymbals
        var filterVal = 0.0

        fun trigger(voiceType: Int, vel: Float = 0.8f) {
            type = voiceType
            active = true
            ageSamples = 0
            velocity = vel.coerceIn(0.1f, 1.0f)
            phase = 0.0
            phase2 = 0.0
            filterVal = 0.0

            decaySamples = when (voiceType) {
                1 -> (SAMPLE_RATE * 0.28).toInt() // Kick (280ms)
                2 -> (SAMPLE_RATE * 0.24).toInt() // Snare (240ms)
                3 -> (SAMPLE_RATE * 0.06).toInt() // Closed Hat (60ms)
                4 -> (SAMPLE_RATE * 0.45).toInt() // Open Hat (450ms)
                5 -> (SAMPLE_RATE * 0.65).toInt() // Ride Cymbal (650ms)
                6 -> (SAMPLE_RATE * 1.20).toInt() // Crash Cymbal (1.2s explosion!)
                7 -> (SAMPLE_RATE * 0.09).toInt() // Rimshot (90ms)
                8 -> (SAMPLE_RATE * 0.32).toInt() // Tom (320ms)
                9 -> (SAMPLE_RATE * 0.20).toInt() // Cajon Thud
                10 -> (SAMPLE_RATE * 0.12).toInt() // Cajon Slap
                else -> (SAMPLE_RATE * 0.10).toInt()
            }
        }

        fun nextSample(): Float {
            if (!active) return 0f
            ageSamples++
            if (ageSamples >= decaySamples) {
                active = false
                return 0f
            }

            val t = ageSamples.toDouble() / SAMPLE_RATE
            val progress = ageSamples.toDouble() / decaySamples
            val progressInv = (1.0 - progress).coerceAtLeast(0.0)

            return when (type) {
                1 -> { // Studio Punchy Kick: Fast pitch sweep + wood beater attack + 50Hz punch body
                    val pitchEnv = exp(-t / 0.028)
                    val freq = 48.0 + 120.0 * pitchEnv
                    phase += (2.0 * PI * freq) / SAMPLE_RATE
                    val beaterClick = rng.nextNoise() * exp(-t / 0.005) * 0.25
                    val body = sin(phase) * (progressInv * progressInv)
                    ((body + beaterClick) * 0.85 * velocity).toFloat()
                }

                2 -> { // Snare: Crisp rim transient + 185Hz woody shell + snare wire sizzle
                    val shellFreq = 185.0 * exp(-t / 0.035)
                    phase += (2.0 * PI * shellFreq) / SAMPLE_RATE
                    val shell = sin(phase) * exp(-t / 0.07) * 0.45
                    val rawNoise = rng.nextNoise()
                    // Snare wire bandpass filtering
                    filterVal += (rawNoise - filterVal) * 0.45
                    val wires = filterVal * exp(-t / 0.14) * 0.50
                    val rimSnap = exp(-t / 0.006) * 0.35
                    ((shell + wires + rimSnap) * 0.80 * velocity).toFloat()
                }

                3 -> { // Closed Hi-Hat: Crisp stick transient + bandpassed bronze ring
                    val raw = rng.nextNoise()
                    filterVal += (raw - filterVal) * 0.75
                    val sizzle = (raw - filterVal * 0.3) * exp(-t / 0.022) * 0.40
                    phase += (2.0 * PI * 8500.0) / SAMPLE_RATE
                    val metallic = sin(phase) * exp(-t / 0.015) * 0.15
                    ((sizzle + metallic) * velocity).toFloat()
                }

                4 -> { // Open Hi-Hat: Sizzling metallic wash with gradual decay
                    val raw = rng.nextNoise()
                    filterVal += (raw - filterVal) * 0.65
                    val wash = filterVal * exp(-t / 0.22) * 0.45
                    phase += (2.0 * PI * 7200.0) / SAMPLE_RATE
                    val chime = sin(phase) * exp(-t / 0.12) * 0.12
                    ((wash + chime) * velocity).toFloat()
                }

                5 -> { // Ride Cymbal: Bell ping + bronze sustain
                    phase += (2.0 * PI * 920.0) / SAMPLE_RATE
                    phase2 += (2.0 * PI * 1480.0) / SAMPLE_RATE
                    val bell = (sin(phase) * 0.6 + sin(phase2) * 0.4) * exp(-t / 0.25) * 0.28
                    val raw = rng.nextNoise()
                    filterVal += (raw - filterVal) * 0.5
                    val shimmer = filterVal * exp(-t / 0.40) * 0.22
                    ((bell + shimmer) * velocity).toFloat()
                }

                6 -> { // Crash Cymbal: Huge explosive transient + long high-frequency wash
                    val raw = rng.nextNoise()
                    filterVal += (raw - filterVal) * 0.55
                    val wash = filterVal * exp(-t / 0.60) * 0.55
                    phase += (2.0 * PI * 650.0) / SAMPLE_RATE
                    phase2 += (2.0 * PI * 1150.0) / SAMPLE_RATE
                    val bronzeRing = (sin(phase) * 0.4 + sin(phase2) * 0.3) * exp(-t / 0.35) * 0.25
                    ((wash + bronzeRing) * velocity * 0.90).toFloat()
                }

                7 -> { // Rimshot / Woodblock: Sharp resonant rim hit
                    phase += (2.0 * PI * 740.0) / SAMPLE_RATE
                    val tone = sin(phase) * exp(-t / 0.025) * 0.65
                    val snap = rng.nextNoise() * exp(-t / 0.008) * 0.35
                    ((tone + snap) * velocity).toFloat()
                }

                8 -> { // Tom: Pitch drop acoustic rack/floor tom
                    val tomFreq = 130.0 * exp(-t / 0.05) + 65.0
                    phase += (2.0 * PI * tomFreq) / SAMPLE_RATE
                    val body = sin(phase) * exp(-t / 0.12) * 0.70
                    val stick = rng.nextNoise() * exp(-t / 0.008) * 0.30
                    ((body + stick) * velocity).toFloat()
                }

                9 -> { // Flamenco Cajon Thud: Deep resonance cavity
                    phase += (2.0 * PI * 95.0) / SAMPLE_RATE
                    val boom = sin(phase) * exp(-t / 0.08) * 0.75
                    (boom * velocity).toFloat()
                }

                10 -> { // Flamenco Cajon Slap: High-end palm snap + wood rattle
                    phase += (2.0 * PI * 350.0) / SAMPLE_RATE
                    val wood = sin(phase) * exp(-t / 0.03) * 0.40
                    val slap = rng.nextNoise() * exp(-t / 0.02) * 0.60
                    ((wood + slap) * velocity).toFloat()
                }

                else -> 0f
            }
        }
    }

    /**
     * Accompaniment Wavetable Voice for Backing Track Bass & Chords.
     */
    class AccompanimentWavetableVoice {
        var active = false
        var freq = 110.0
        var phase = 0.0
        var ageSamples = 0
        var durationSamples = 0
        var velocity = 0.7f
        var instrumentTable: FloatArray = electricBassTable
        var pluckTransient = 0.0
        var isBass = false
        private val rng = FastRng()

        fun trigger(midiPitch: Int, durationMs: Int, table: FloatArray, vel: Float = 0.7f, bassMode: Boolean = false) {
            freq = (440.0 * 2.0.pow((midiPitch - 69.0) / 12.0))
            durationSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt().coerceAtLeast(100)
            instrumentTable = table
            velocity = vel.coerceIn(0.1f, 1.0f)
            ageSamples = 0
            active = true
            phase = 0.0
            isBass = bassMode
            pluckTransient = 1.0
        }

        fun nextSample(): Float {
            if (!active) return 0f
            ageSamples++
            if (ageSamples >= durationSamples) {
                active = false
                return 0f
            }

            val t = ageSamples.toDouble() / SAMPLE_RATE
            val progress = ageSamples.toDouble() / durationSamples
            val phaseDelta = (2.0 * PI * freq) / SAMPLE_RATE
            phase = (phase + phaseDelta) % (2.0 * PI)

            // Wavetable lookup with linear interpolation
            val wtSample = readWavetable(instrumentTable, phase)

            // Attack transient (pluck click / hammer tap)
            val transientDuration = if (isBass) 0.012 else 0.008
            val click = if (t < transientDuration) {
                rng.nextNoise() * (1.0 - t / transientDuration) * (if (isBass) 0.25 else 0.15)
            } else 0.0

            // Envelope decay
            val decayTime = if (isBass) 0.70 else 0.55
            val env = exp(-t / decayTime).coerceAtLeast(0.0)

            val out = (wtSample * env + click) * velocity * 0.75
            return out.toFloat()
        }
    }

    /**
     * Strum Manager: Delays individual chord notes across strings (12ms - 20ms)
     * to transform robotic block chords into humanized acoustic guitar strums and piano rolls!
     */
    class StrumNote(
        var midi: Int,
        var delaySamples: Int,
        var durationSamples: Int,
        var velocity: Float,
        var table: FloatArray
    )
}
