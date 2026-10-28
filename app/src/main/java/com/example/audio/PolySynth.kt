package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.model.LeadInstrument
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*

class PolySynth {
    companion object {
        const val SAMPLE_RATE = 44100
        private const val BUFFER_SIZE = 1024
        private const val MAX_VOICES = 8
    }

    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null
    private val isRunning = AtomicBoolean(false)

    // Synthesizer Parameters
    @Volatile var activeInstrument: LeadInstrument = LeadInstrument.OVERDRIVE_GUITAR
    @Volatile var masterVolume: Float = 0.85f
    @Volatile var soloVolume: Float = 0.90f
    @Volatile var isSoloMuted: Boolean = false
    @Volatile var driveAmount: Float = 0.5f // 0.0 to 1.0
    @Volatile var filterCutoff: Float = 0.7f // 0.0 to 1.0
    @Volatile var vibratoAmount: Float = 0.3f // 0.0 to 1.0
    @Volatile var pitchBendSemitones: Float = 0.0f // -2.0 to +2.0

    // Waveform visual buffer for Compose UI
    private val visualBuffer = FloatArray(128)
    private val visualLock = Any()

    fun getVisualWaveform(): FloatArray {
        synchronized(visualLock) {
            return visualBuffer.copyOf()
        }
    }

    private class Voice(
        var id: Long = 0,
        var midiNote: Int = 60,
        var baseFreq: Float = 261.63f,
        var velocity: Float = 0.8f,
        var active: Boolean = false,
        var isReleasing: Boolean = false,
        var ageSamples: Long = 0,
        var phase: Double = 0.0,
        var phase2: Double = 0.0,
        var phaseSub: Double = 0.0,
        var lfoPhase: Double = 0.0,
        var envLevel: Float = 0.0f,
        // State-variable filter registers
        var filterLow: Double = 0.0,
        var filterBand: Double = 0.0
    )

    private val voices = Array(MAX_VOICES) { Voice() }
    private var nextVoiceId = 1L

    private sealed class SynthCommand {
        data class NoteOn(val midiNote: Int, val velocity: Float) : SynthCommand()
        data class NoteOff(val midiNote: Int) : SynthCommand()
        object AllNotesOff : SynthCommand()
    }

    private val commandQueue = ConcurrentLinkedQueue<SynthCommand>()

    fun start() {
        if (isRunning.get()) return

        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferBytes = max(minBuf, BUFFER_SIZE * 2 * 2)

        audioTrack = AudioTrack.Builder()
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

        isRunning.set(true)
        audioTrack?.play()

        renderThread = Thread({ renderLoop() }, "JamspiratePolySynthThread").apply {
            priority = Thread.MAX_PRIORITY
            start()
        }
    }

    fun stop() {
        isRunning.set(false)
        renderThread?.interrupt()
        renderThread = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun noteOn(midiNote: Int, velocity: Float = 0.8f) {
        commandQueue.offer(SynthCommand.NoteOn(midiNote, velocity))
    }

    fun noteOff(midiNote: Int) {
        commandQueue.offer(SynthCommand.NoteOff(midiNote))
    }

    fun allNotesOff() {
        commandQueue.offer(SynthCommand.AllNotesOff)
    }

    private fun handleCommands() {
        while (true) {
            val cmd = commandQueue.poll() ?: break
            when (cmd) {
                is SynthCommand.NoteOn -> {
                    // Reuse existing voice for same note or steal oldest/inactive
                    var targetVoice = voices.find { it.active && it.midiNote == cmd.midiNote }
                    if (targetVoice == null) {
                        targetVoice = voices.find { !it.active }
                    }
                    if (targetVoice == null) {
                        // Steal voice with lowest envelope or oldest
                        targetVoice = voices.minByOrNull { if (it.isReleasing) 0f else it.envLevel } ?: voices[0]
                    }

                    targetVoice.apply {
                        id = nextVoiceId++
                        midiNote = cmd.midiNote
                        baseFreq = (440.0 * 2.0.pow((cmd.midiNote - 69.0) / 12.0)).toFloat()
                        velocity = cmd.velocity.coerceIn(0.1f, 1.0f)
                        active = true
                        isReleasing = false
                        ageSamples = 0
                        phase = 0.0
                        phase2 = 0.0
                        phaseSub = 0.0
                        lfoPhase = 0.0
                        filterLow = 0.0
                        filterBand = 0.0
                    }
                }
                is SynthCommand.NoteOff -> {
                    voices.filter { it.active && it.midiNote == cmd.midiNote }.forEach {
                        it.isReleasing = true
                    }
                }
                is SynthCommand.AllNotesOff -> {
                    voices.forEach { it.isReleasing = true }
                }
            }
        }
    }

    private fun renderLoop() {
        val audioBuffer = ShortArray(BUFFER_SIZE)
        val floatSamples = FloatArray(BUFFER_SIZE)

        while (isRunning.get() && !Thread.currentThread().isInterrupted) {
            handleCommands()

            val inst = activeInstrument
            val masterVol = masterVolume
            val drive = driveAmount
            val cutoffParam = filterCutoff
            val vibDepth = vibratoAmount
            val bendFactor = 2.0.pow(pitchBendSemitones.toDouble() / 12.0)

            floatSamples.fill(0f)

            for (v in voices) {
                if (!v.active) continue

                val targetFreq = (v.baseFreq * bendFactor).toFloat()

                // Calculate ADSR parameters based on instrument
                val (attackTime, decayTime, sustainLevel, releaseTime) = getAdsr(inst)
                val attackSamples = (attackTime * SAMPLE_RATE).toInt()
                val decaySamples = (decayTime * SAMPLE_RATE).toInt()
                val releaseSamples = (releaseTime * SAMPLE_RATE).toInt()

                val lfoSpeed = when (inst) {
                    LeadInstrument.TENOR_SAX -> 5.5 // classic vibrato
                    LeadInstrument.JAZZ_TRUMPET -> 6.0
                    LeadInstrument.HIGH_GAIN_GUITAR -> 6.5
                    LeadInstrument.OVERDRIVE_GUITAR -> 5.2
                    else -> 4.5
                }
                val lfoDelta = (2.0 * PI * lfoSpeed) / SAMPLE_RATE

                for (i in 0 until BUFFER_SIZE) {
                    v.ageSamples++

                    // ADSR Envelope calculation
                    if (!v.isReleasing) {
                        if (v.ageSamples < attackSamples) {
                            v.envLevel = (v.ageSamples.toFloat() / attackSamples).coerceIn(0f, 1f)
                        } else if (v.ageSamples < attackSamples + decaySamples) {
                            val decayProgress = (v.ageSamples - attackSamples).toFloat() / decaySamples
                            v.envLevel = 1.0f - decayProgress * (1.0f - sustainLevel)
                        } else {
                            v.envLevel = sustainLevel
                        }
                    } else {
                        v.envLevel -= (1.0f / releaseSamples)
                        if (v.envLevel <= 0.001f) {
                            v.envLevel = 0f
                            v.active = false
                            break
                        }
                    }

                    // LFO Vibrato
                    v.lfoPhase += lfoDelta
                    val vibMod = if (inst == LeadInstrument.PRODIGY_RAVE_SYNTH) {
                        0.0
                    } else {
                        sin(v.lfoPhase) * 0.03 * vibDepth
                    }
                    val currentFreq = (targetFreq * (1.0 + vibMod)).coerceAtLeast(20.0)
                    val phaseDelta = (2.0 * PI * currentFreq) / SAMPLE_RATE

                    // Generate Raw Oscillator Sample
                    var sample = generateSample(inst, v, phaseDelta, currentFreq)

                    // Apply filter if applicable
                    sample = applyFilter(inst, v, sample, cutoffParam)

                    // Apply Drive / Distortion
                    sample = applyDistortion(inst, sample, drive)

                    floatSamples[i] += (sample * v.envLevel * v.velocity).toFloat()
                }
            }

            // Master limiting and write to ShortArray for AudioTrack
            val effectiveSoloVol = if (isSoloMuted) 0f else soloVolume
            var sampleSum = 0f
            for (i in 0 until BUFFER_SIZE) {
                var s = floatSamples[i] * effectiveSoloVol * masterVol
                // Soft clipping limiter
                s = tanh(s.toDouble()).toFloat()
                audioBuffer[i] = (s * 32767f).coerceIn(-32768f, 32767f).toInt().toShort()
                sampleSum += abs(s)
            }

            // Downsample for visualizer
            synchronized(visualLock) {
                val step = BUFFER_SIZE / 128
                for (j in 0 until 128) {
                    visualBuffer[j] = floatSamples[j * step] * masterVol
                }
            }

            audioTrack?.write(audioBuffer, 0, BUFFER_SIZE)
        }
    }

    private fun getAdsr(inst: LeadInstrument): FloatArray {
        return when (inst) {
            LeadInstrument.PRODIGY_RAVE_SYNTH -> floatArrayOf(0.008f, 0.15f, 0.85f, 0.10f)
            LeadInstrument.HIGH_GAIN_GUITAR -> floatArrayOf(0.015f, 0.20f, 0.75f, 0.25f)
            LeadInstrument.OVERDRIVE_GUITAR -> floatArrayOf(0.025f, 0.35f, 0.60f, 0.30f)
            LeadInstrument.TENOR_SAX -> floatArrayOf(0.060f, 0.20f, 0.80f, 0.15f)
            LeadInstrument.FLAMENCO_GUITAR -> floatArrayOf(0.005f, 0.25f, 0.10f, 0.12f)
            LeadInstrument.JAZZ_TRUMPET -> floatArrayOf(0.035f, 0.15f, 0.75f, 0.12f)
            LeadInstrument.RHODES_PIANO -> floatArrayOf(0.010f, 0.60f, 0.35f, 0.50f)
            LeadInstrument.GRAND_PIANO -> floatArrayOf(0.006f, 0.80f, 0.25f, 0.40f)
        }
    }

    private fun generateSample(
        inst: LeadInstrument,
        v: Voice,
        phaseDelta: Double,
        currentFreq: Double
    ): Double {
        v.phase = (v.phase + phaseDelta) % (2.0 * PI)
        val t = v.ageSamples.toDouble() / SAMPLE_RATE

        return when (inst) {
            LeadInstrument.PRODIGY_RAVE_SYNTH -> {
                // Aggressive Wavetable dual unison saw with sub-bass weight
                val wt1 = WavetableSoundfont.readWavetable(WavetableSoundfont.raveSynthTable, v.phase)
                v.phase2 = (v.phase2 + phaseDelta * 1.008) % (2.0 * PI)
                val wt2 = WavetableSoundfont.readWavetable(WavetableSoundfont.raveSynthTable, v.phase2)
                v.phaseSub = (v.phaseSub + phaseDelta * 0.5) % (2.0 * PI)
                val sub = sin(v.phaseSub) * 0.35
                (wt1 * 0.50 + wt2 * 0.45 + sub)
            }
            LeadInstrument.HIGH_GAIN_GUITAR -> {
                // Saturated Wavetable + pick attack transient
                val wt = WavetableSoundfont.readWavetable(WavetableSoundfont.highGainGuitarTable, v.phase)
                val pickTransient = if (t < 0.012) {
                    val noise = ((v.id * 1664525L + v.ageSamples * 1013904223L) and 0xFFFFL).toDouble() / 32768.0 - 1.0
                    noise * exp(-t / 0.004) * 0.35
                } else 0.0
                val harm2 = sin(v.phase * 2.0) * 0.20
                (wt * 0.85 + pickTransient + harm2)
            }
            LeadInstrument.OVERDRIVE_GUITAR -> {
                // Warm Strat neck pickup Wavetable + touch sensitivity
                val wt = WavetableSoundfont.readWavetable(WavetableSoundfont.overdriveGuitarTable, v.phase)
                val pickTransient = if (t < 0.010) {
                    sin(v.phase * 5.0) * exp(-t / 0.004) * 0.25
                } else 0.0
                (wt * 0.90 + pickTransient)
            }
            LeadInstrument.TENOR_SAX -> {
                // Reedy bore Wavetable + breathy air component
                val wt = WavetableSoundfont.readWavetable(WavetableSoundfont.tenorSaxTable, v.phase)
                val breath = if (t < 0.040) {
                    val noise = ((v.ageSamples * 1103515245L) and 0xFFFFL).toDouble() / 32768.0 - 1.0
                    noise * exp(-t / 0.025) * 0.18
                } else {
                    val gentleNoise = ((v.ageSamples * 1103515245L) and 0xFFFFL).toDouble() / 32768.0 - 1.0
                    gentleNoise * 0.035
                }
                (wt * 0.88 + breath)
            }
            LeadInstrument.FLAMENCO_GUITAR -> {
                // Spanish nylon Wavetable + crisp fingernail picado transient
                val wt = WavetableSoundfont.readWavetable(WavetableSoundfont.flamencoGuitarTable, v.phase)
                val picadoSnap = if (t < 0.008) {
                    sin(v.phase * 6.0) * exp(-t / 0.003) * 0.40
                } else 0.0
                (wt * 0.88 + picadoSnap)
            }
            LeadInstrument.JAZZ_TRUMPET -> {
                // Harmon muted brass Wavetable + lip buzz attack
                val wt = WavetableSoundfont.readWavetable(WavetableSoundfont.jazzTrumpetTable, v.phase)
                val lipBuzz = if (t < 0.015) {
                    (if (v.phase < PI) 0.3 else -0.3) * exp(-t / 0.007)
                } else 0.0
                (wt * 0.90 + lipBuzz)
            }
            LeadInstrument.RHODES_PIANO -> {
                // Fender Rhodes Wavetable + metallic tine chime transient
                val wt = WavetableSoundfont.readWavetable(WavetableSoundfont.rhodesTable, v.phase)
                val chime = if (t < 0.035) {
                    sin(v.phase * 7.0) * exp(-t / 0.018) * 0.30
                } else 0.0
                (wt * 0.88 + chime)
            }
            LeadInstrument.GRAND_PIANO -> {
                // Concert Grand Piano Wavetable + felt hammer attack
                val wt = WavetableSoundfont.readWavetable(WavetableSoundfont.grandPianoTable, v.phase)
                val hammer = if (t < 0.010) {
                    val thud = sin(v.phase * 0.5) * exp(-t / 0.005) * 0.25
                    val click = sin(v.phase * 4.0) * exp(-t / 0.004) * 0.18
                    thud + click
                } else 0.0
                (wt * 0.90 + hammer)
            }
        }
    }

    private fun applyFilter(
        inst: LeadInstrument,
        v: Voice,
        input: Double,
        cutoffParam: Float
    ): Double {
        // State Variable Filter (Chamberlin SVF)
        val baseCutoffHz = when (inst) {
            LeadInstrument.PRODIGY_RAVE_SYNTH -> {
                // Filter envelope sweep for aggressive bite
                val envSweep = exp(-v.ageSamples.toDouble() / (SAMPLE_RATE * 0.18))
                (800.0 + (cutoffParam * 6000.0) + envSweep * 4500.0).coerceIn(200.0, 16000.0)
            }
            LeadInstrument.HIGH_GAIN_GUITAR -> {
                // Cab lowpass roll-off at 4500Hz
                (1200.0 + cutoffParam * 4000.0).coerceIn(400.0, 5000.0)
            }
            LeadInstrument.JAZZ_TRUMPET -> {
                // Harmon mute peak around 2000Hz
                (1000.0 + cutoffParam * 2200.0).coerceIn(600.0, 4000.0)
            }
            LeadInstrument.TENOR_SAX -> {
                (800.0 + cutoffParam * 3000.0).coerceIn(400.0, 4500.0)
            }
            else -> {
                (600.0 + cutoffParam * 10000.0).coerceIn(300.0, 18000.0)
            }
        }

        val q = when (inst) {
            LeadInstrument.PRODIGY_RAVE_SYNTH -> 3.2 // High aggressive resonance
            LeadInstrument.JAZZ_TRUMPET -> 2.5     // Harmon resonance
            LeadInstrument.HIGH_GAIN_GUITAR -> 1.4 // Speaker cab resonance
            else -> 0.9
        }

        val f = (2.0 * sin(PI * baseCutoffHz / SAMPLE_RATE)).coerceIn(0.01, 0.99)
        val damping = 1.0 / q

        v.filterLow += f * v.filterBand
        val high = input - v.filterLow - damping * v.filterBand
        v.filterBand += f * high

        return when (inst) {
            LeadInstrument.JAZZ_TRUMPET -> v.filterBand * 1.5 + v.filterLow * 0.4
            LeadInstrument.PRODIGY_RAVE_SYNTH -> v.filterLow * 1.2 + v.filterBand * 0.5
            else -> v.filterLow
        }
    }

    private fun applyDistortion(inst: LeadInstrument, sample: Double, drive: Float): Double {
        return when (inst) {
            LeadInstrument.PRODIGY_RAVE_SYNTH -> {
                // Hard driven overdrive with subtle edge
                val gain = 1.0 + drive * 5.0
                val s = sample * gain
                tanh(s) * 1.15
            }
            LeadInstrument.HIGH_GAIN_GUITAR -> {
                // Asymmetrical diode clipping
                val gain = 2.0 + drive * 8.0
                val s = sample * gain
                val clipped = if (s > 0) {
                    tanh(s * 1.2)
                } else {
                    -tanh(-s * 0.8)
                }
                clipped * 0.9
            }
            LeadInstrument.OVERDRIVE_GUITAR -> {
                val gain = 1.2 + drive * 3.5
                val s = sample * gain
                tanh(s)
            }
            else -> sample
        }
    }
}
