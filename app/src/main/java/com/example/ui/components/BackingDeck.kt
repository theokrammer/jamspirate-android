package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackingSourceType
import com.example.model.BackingTrack

@Composable
fun BackingDeck(
    activeTrack: BackingTrack,
    allTracks: List<BackingTrack>,
    isJamming: Boolean,
    isMicRecording: Boolean,
    backingVolume: Float,
    masterVolume: Float,
    onSelectTrack: (BackingTrack) -> Unit,
    onToggleJam: () -> Unit,
    onToggleMicRecording: () -> Unit,
    onImportFile: (android.net.Uri, String) -> Unit,
    onSetBackingVolume: (Float) -> Unit,
    onSetMasterVolume: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // File picker launcher for MIDI, MP3, OGG, WAV
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "Imported_Track"
            onImportFile(uri, fileName)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("backing_deck_card"),
        shape = RoundedCornerShape(14.dp),
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
            // Header with Master Play/Stop & Mic Loop Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "ACTIVE TRACK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                    Text(
                        text = "${activeTrack.title} (${activeTrack.bpm} BPM • ${activeTrack.timeSignature})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Import Audio/MIDI Button
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("button_import_track")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = "Import Audio or MIDI",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Import", fontSize = 10.sp)
                    }

                    // Record Live Mic Button
                    val micBg by animateColorAsState(
                        targetValue = if (isMicRecording) Color(0xFFEF4444) else Color(0xFF1E293B),
                        animationSpec = tween(150),
                        label = "mic_rec_color"
                    )
                    Button(
                        onClick = onToggleMicRecording,
                        colors = ButtonDefaults.buttonColors(containerColor = micBg),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("button_record_mic")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Record Live Mic",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isMicRecording) "Stop Loop" else "Record Mic",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Presets Track Carousel
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("preset_tracks_row"),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(allTracks, key = { it.id }) { track ->
                    val isSelected = track.id == activeTrack.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectTrack(track) },
                        label = {
                            Text(
                                text = "${track.title} • ${track.timeSignature}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF22D3EE).copy(alpha = 0.25f),
                            selectedLabelColor = Color(0xFF22D3EE)
                        ),
                        modifier = Modifier.testTag("track_chip_${track.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Master Transport & Volume Faders
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Large Master Start Jam Button
                Button(
                    onClick = onToggleJam,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isJamming) Color(0xFFDC2626) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(50.dp)
                        .weight(1f)
                        .testTag("button_master_jam_toggle")
                ) {
                    Icon(
                        imageVector = if (isJamming) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isJamming) "STOP JAM" else "START JAM",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Volume Controls
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Backing Vol",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = "${(backingVolume * 100).toInt()}%",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Slider(
                        value = backingVolume,
                        onValueChange = onSetBackingVolume,
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF22D3EE),
                            activeTrackColor = Color(0xFF22D3EE)
                        ),
                        modifier = Modifier.testTag("slider_backing_volume")
                    )
                }
            }
        }
    }
}
