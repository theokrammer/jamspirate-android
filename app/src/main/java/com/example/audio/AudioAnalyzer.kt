package com.example.audio

import com.example.engine.MusicTheory
import com.example.model.ScaleType
import kotlin.math.*

class AudioAnalyzer(private val sampleRate: Int = 44100) {

    data class AnalysisResult(
        val pitchHz: Float,
        val noteName: String,
        val midiNote: Int,
        val cents: Float,
        val confidence: Float,
        val estimatedBpm: Int,
        val isBeatOnset: Boolean,
        val detectedKey: String,
        val detectedScale: ScaleType,
        val energyRms: Float
    )

    private val windowSize = 2048
    private val yinBuffer = FloatArray(windowSize / 2)
    private var lastBeatTimestamp = 0L
    private val recentIntervals = mutableListOf<Long>()
    private var smoothedBpm = 120

    // Energy tracking for onset detection
    private var prevEnergy = 0f
    private var energyThreshold = 0.015f

    // Pitch Chroma distribution (12 semitones)
    private val chromaBins = FloatArray(12)

    fun processAudioFrame(samples: ShortArray, length: Int): AnalysisResult {
        if (length < windowSize) {
            return fallbackResult()
        }

        // 1. Calculate RMS Energy
        var sumSquares = 0.0
        val floatBuf = FloatArray(windowSize)
        for (i in 0 until windowSize) {
            val s = samples[i] / 32768.0f
            floatBuf[i] = s
            sumSquares += s * s
        }
        val rms = sqrt(sumSquares / windowSize).toFloat()

        // 2. Beat / Onset Detection via Energy Flux
        var isBeat = false
        val now = System.currentTimeMillis()
        val energyDelta = (rms - prevEnergy).coerceAtLeast(0f)
        val dynamicThreshold = max(0.025f, prevEnergy * 0.4f)
        if (energyDelta > dynamicThreshold && (now - lastBeatTimestamp) > 280) {
            val interval = now - lastBeatTimestamp
            lastBeatTimestamp = now
            isBeat = true

            if (interval in 300..1200) {
                recentIntervals.add(interval)
                if (recentIntervals.size > 10) recentIntervals.removeAt(0)
                // Remove min and max outliers
                val sorted = recentIntervals.sorted()
                val trimmed = if (sorted.size >= 4) sorted.subList(1, sorted.size - 1) else sorted
                val avgInterval = trimmed.average()
                if (avgInterval > 0) {
                    val rawBpm = (60000.0 / avgInterval).roundToInt().coerceIn(60, 200)
                    smoothedBpm = (smoothedBpm * 0.85 + rawBpm * 0.15).roundToInt()
                }
            }
        }
        prevEnergy = prevEnergy * 0.88f + rms * 0.12f

        // 3. Pitch Detection via YIN Algorithm
        var pitchHz = 0f
        var confidence = 0f

        if (rms > 0.01f) {
            val halfWindow = windowSize / 2

            // Step 1: Difference Function
            yinBuffer[0] = 1f
            for (tau in 1 until halfWindow) {
                var diff = 0f
                for (i in 0 until halfWindow) {
                    val d = floatBuf[i] - floatBuf[i + tau]
                    diff += d * d
                }
                yinBuffer[tau] = diff
            }

            // Step 2: Cumulative Mean Normalized Difference
            var runningSum = 0f
            yinBuffer[0] = 1f
            for (tau in 1 until halfWindow) {
                runningSum += yinBuffer[tau]
                yinBuffer[tau] = if (runningSum > 0f) {
                    (yinBuffer[tau] * tau) / runningSum
                } else {
                    1f
                }
            }

            // Step 3: Absolute Thresholding
            val threshold = 0.18f
            var bestTau = -1
            for (tau in 2 until halfWindow) {
                if (yinBuffer[tau] < threshold) {
                    var localTau = tau
                    while (localTau + 1 < halfWindow && yinBuffer[localTau + 1] < yinBuffer[localTau]) {
                        localTau++
                    }
                    bestTau = localTau
                    break
                }
            }

            // If no valley below threshold, pick absolute minimum
            if (bestTau == -1) {
                var minVal = Float.MAX_VALUE
                for (tau in 2 until halfWindow) {
                    if (yinBuffer[tau] < minVal) {
                        minVal = yinBuffer[tau]
                        bestTau = tau
                    }
                }
            }

            if (bestTau in 1 until halfWindow - 1) {
                // Step 4: Parabolic Interpolation for Sub-sample accuracy
                val s0 = yinBuffer[bestTau - 1]
                val s1 = yinBuffer[bestTau]
                val s2 = yinBuffer[bestTau + 1]
                val denom = 2f * (s0 - 2f * s1 + s2)
                val delta = if (denom != 0f) (s0 - s2) / denom else 0f
                val preciseTau = bestTau + delta

                if (preciseTau > 0f) {
                    val rawHz = sampleRate / preciseTau
                    if (rawHz in 55.0..1800.0) { // Musical range A1 to A6
                        pitchHz = rawHz
                        confidence = (1.0f - yinBuffer[bestTau]).coerceIn(0f, 1f)
                    }
                }
            }
        }

        // 4. Chroma Accumulation for Key/Scale Estimation
        var noteName = "--"
        var midiNote = 0
        var cents = 0f

        if (pitchHz > 0f && confidence > 0.4f) {
            val rawMidi = MusicTheory.frequencyToMidi(pitchHz)
            midiNote = rawMidi.roundToInt()
            cents = (rawMidi - midiNote) * 100f
            noteName = MusicTheory.midiToNoteName(midiNote)

            val pitchClass = (midiNote % 12).coerceIn(0, 11)
            chromaBins[pitchClass] += confidence * rms
        }

        // Decay chroma slowly
        for (i in 0 until 12) {
            chromaBins[i] *= 0.992f
        }

        val estimatedKey = estimateKeyCenter()

        return AnalysisResult(
            pitchHz = pitchHz,
            noteName = noteName,
            midiNote = midiNote,
            cents = cents,
            confidence = confidence,
            estimatedBpm = smoothedBpm,
            isBeatOnset = isBeat,
            detectedKey = estimatedKey.first,
            detectedScale = estimatedKey.second,
            energyRms = rms
        )
    }

    private fun estimateKeyCenter(): Pair<String, ScaleType> {
        val noteNames = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        var bestScore = -1f
        var bestRoot = 0
        var bestScale = ScaleType.DORIAN

        // Evaluate minor, dorian, blues profiles against chroma bins
        for (root in 0 until 12) {
            // Dorian profile
            val dorianWeight = floatArrayOf(2f, 0f, 1.2f, 1.8f, 0f, 1.5f, 0f, 1.8f, 0f, 1.2f, 1.5f, 0f)
            var dorianScore = 0f
            for (step in 0 until 12) {
                dorianScore += chromaBins[(root + step) % 12] * dorianWeight[step]
            }

            // Blues profile
            val bluesWeight = floatArrayOf(2.2f, 0f, 0f, 1.8f, 0f, 1.5f, 1.4f, 1.8f, 0f, 0f, 1.6f, 0f)
            var bluesScore = 0f
            for (step in 0 until 12) {
                bluesScore += chromaBins[(root + step) % 12] * bluesWeight[step]
            }

            if (dorianScore > bestScore) {
                bestScore = dorianScore
                bestRoot = root
                bestScale = ScaleType.DORIAN
            }
            if (bluesScore > bestScore) {
                bestScore = bluesScore
                bestRoot = root
                bestScale = ScaleType.BLUES
            }
        }

        return Pair(noteNames[bestRoot], bestScale)
    }

    private fun fallbackResult(): AnalysisResult {
        return AnalysisResult(
            pitchHz = 0f,
            noteName = "--",
            midiNote = 0,
            cents = 0f,
            confidence = 0f,
            estimatedBpm = smoothedBpm,
            isBeatOnset = false,
            detectedKey = "A",
            detectedScale = ScaleType.DORIAN,
            energyRms = 0f
        )
    }
}
