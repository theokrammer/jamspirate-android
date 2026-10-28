package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import com.example.model.LeadInstrument

@Composable
fun InstrumentDeck(
    activeInstrument: LeadInstrument,
    driveAmount: Float,
    filterCutoff: Float,
    soloDensity: Float,
    creativity: Float,
    isSoloMuted: Boolean = false,
    soloVolume: Float = 0.90f,
    onSelectInstrument: (LeadInstrument) -> Unit,
    onSetDrive: (Float) -> Unit,
    onSetFilterCutoff: (Float) -> Unit,
    onSetSoloDensity: (Float) -> Unit,
    onSetCreativity: (Float) -> Unit,
    onSetPitchBend: (Float) -> Unit,
    onTriggerNote: (Int) -> Unit,
    onToggleSoloMute: () -> Unit = {},
    onSetSoloVolume: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("instrument_deck_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131A29)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E293B))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = activeInstrument.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PRESET VOICE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(activeInstrument.accentColor.copy(alpha = 0.2f))
                        .border(1.dp, activeInstrument.accentColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = activeInstrument.displayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeInstrument.accentColor,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Instrument Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("instrument_chips_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(LeadInstrument.values()) { inst ->
                    val isSelected = inst == activeInstrument
                    ElevatedFilterChip(
                        selected = isSelected,
                        onClick = { onSelectInstrument(inst) },
                        label = {
                            Text(
                                text = inst.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            selectedContainerColor = inst.accentColor.copy(alpha = 0.25f),
                            selectedLabelColor = inst.accentColor
                        ),
                        modifier = Modifier.testTag("instrument_chip_${inst.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Real-time Tone Sculpting Sliders (2x2 balanced layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Column 1: Drive & Filter Cutoff
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Drive / Distortion",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(driveAmount * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = driveAmount,
                        onValueChange = onSetDrive,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFEF4444),
                            activeTrackColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.testTag("slider_drive")
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Filter Cutoff",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(filterCutoff * 100).toInt()}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF84CC16),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = filterCutoff,
                        onValueChange = onSetFilterCutoff,
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF84CC16),
                            activeTrackColor = Color(0xFF84CC16)
                        ),
                        modifier = Modifier.testTag("slider_cutoff")
                    )
                }

                // Column 2: Solo Density & Harmonic Adventure (Creativity)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Solo Density",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(soloDensity * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = soloDensity,
                        onValueChange = onSetSoloDensity,
                        valueRange = 0.2f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8)
                        ),
                        modifier = Modifier.testTag("slider_density")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Harmonic Adventure",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (creativity > 0.6f) "Outside" else "In-Key",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA855F7),
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = creativity,
                        onValueChange = onSetCreativity,
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFA855F7),
                            activeTrackColor = Color(0xFFA855F7)
                        ),
                        modifier = Modifier.testTag("slider_creativity")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Touch Expression Pitch Bend Strip & Quick Audition Keys
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Pitch bend ribbon
                var bendOffset by remember { mutableStateOf(0f) }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    bendOffset = 0f
                                    onSetPitchBend(0f)
                                },
                                onDragCancel = {
                                    bendOffset = 0f
                                    onSetPitchBend(0f)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    bendOffset = (bendOffset + dragAmount.x / 40f).coerceIn(-2.0f, 2.0f)
                                    onSetPitchBend(bendOffset)
                                }
                            )
                        }
                        .testTag("pitch_bend_ribbon"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (bendOffset != 0f) "BEND: ${String.format("%.1f", bendOffset)} st" else "◄ DRAG TO PITCH BEND ►",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bendOffset != 0f) activeInstrument.accentColor else Color(0xFF64748B),
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Quick Note Audition Keys
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { onTriggerNote(60) }, // C4
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("audition_key_c4")
                    ) {
                        Text("C4", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { onTriggerNote(64) }, // E4
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("audition_key_e4")
                    ) {
                        Text("E4", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    FilledTonalButton(
                        onClick = { onTriggerNote(67) }, // G4
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("audition_key_g4")
                    ) {
                        Text("G4", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
