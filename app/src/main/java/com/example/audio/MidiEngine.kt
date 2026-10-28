package com.example.audio

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MidiEvent(
    val timeTick: Long,
    val type: Int, // 0x90 = NoteOn, 0x80 = NoteOff, 0x51 = SetTempo
    val channel: Int,
    val data1: Int, // note
    val data2: Int  // velocity
)

data class ParsedMidi(
    val format: Int,
    val ticksPerBeat: Int,
    val tempoBpm: Int,
    val events: List<MidiEvent>,
    val durationMs: Long
)

object MidiParser {
    fun parse(stream: InputStream): ParsedMidi? {
        return try {
            val bytes = stream.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN)

            // Header chunk
            val headerTag = ByteArray(4)
            buffer.get(headerTag)
            if (String(headerTag) != "MThd") return null

            val headerLen = buffer.int
            val format = buffer.short.toInt()
            val trackCount = buffer.short.toInt()
            val division = buffer.short.toInt()
            val ticksPerBeat = if (division > 0) division else 480

            // Skip any extra header bytes
            if (headerLen > 6) {
                buffer.position(buffer.position() + (headerLen - 6))
            }

            val allEvents = mutableListOf<MidiEvent>()
            var tempoMicroseconds = 500000 // default 120 BPM

            for (t in 0 until trackCount) {
                if (buffer.remaining() < 8) break
                val trackTag = ByteArray(4)
                buffer.get(trackTag)
                val trackLen = buffer.int
                val trackEnd = buffer.position() + trackLen

                var currentTick = 0L
                var runningStatus = 0

                while (buffer.position() < trackEnd && buffer.hasRemaining()) {
                    val delta = readVariableLength(buffer)
                    currentTick += delta

                    var status = buffer.get().toInt() and 0xFF
                    if (status < 0x80) {
                        // Running status
                        status = runningStatus
                        buffer.position(buffer.position() - 1)
                    } else {
                        runningStatus = status
                    }

                    val messageType = status and 0xF0
                    val channel = status and 0x0F

                    when (messageType) {
                        0x80 -> { // Note Off
                            val note = buffer.get().toInt() and 0x7F
                            val vel = buffer.get().toInt() and 0x7F
                            allEvents.add(MidiEvent(currentTick, 0x80, channel, note, vel))
                        }
                        0x90 -> { // Note On
                            val note = buffer.get().toInt() and 0x7F
                            val vel = buffer.get().toInt() and 0x7F
                            val type = if (vel == 0) 0x80 else 0x90
                            allEvents.add(MidiEvent(currentTick, type, channel, note, vel))
                        }
                        0xA0, 0xB0, 0xE0 -> { // PolyPressure, ControlChange, PitchBend
                            buffer.get()
                            buffer.get()
                        }
                        0xC0, 0xD0 -> { // ProgramChange, ChannelPressure
                            buffer.get()
                        }
                        0xF0 -> { // System Exclusive or Meta
                            if (status == 0xFF) {
                                val metaType = buffer.get().toInt() and 0xFF
                                val metaLen = readVariableLength(buffer).toInt()
                                if (metaType == 0x51 && metaLen == 3) {
                                    val b1 = buffer.get().toInt() and 0xFF
                                    val b2 = buffer.get().toInt() and 0xFF
                                    val b3 = buffer.get().toInt() and 0xFF
                                    tempoMicroseconds = (b1 shl 16) or (b2 shl 8) or b3
                                    allEvents.add(MidiEvent(currentTick, 0x51, 0, tempoMicroseconds, 0))
                                } else {
                                    buffer.position(buffer.position() + metaLen)
                                }
                            } else {
                                val sysExLen = readVariableLength(buffer).toInt()
                                buffer.position(buffer.position() + sysExLen)
                            }
                        }
                    }
                }
            }

            allEvents.sortBy { it.timeTick }
            val bpm = (60000000.0 / tempoMicroseconds).toInt().coerceIn(40, 240)
            val maxTick = allEvents.maxOfOrNull { it.timeTick } ?: 0L
            val durationMs = ((maxTick.toDouble() / ticksPerBeat) * (60000.0 / bpm)).toLong()

            ParsedMidi(
                format = format,
                ticksPerBeat = ticksPerBeat,
                tempoBpm = bpm,
                events = allEvents,
                durationMs = durationMs.coerceAtLeast(1000L)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun readVariableLength(buffer: ByteBuffer): Long {
        var value = 0L
        var byte: Int
        do {
            if (!buffer.hasRemaining()) break
            byte = buffer.get().toInt() and 0xFF
            value = (value shl 7) or (byte and 0x7F).toLong()
        } while ((byte and 0x80) != 0)
        return value
    }
}
