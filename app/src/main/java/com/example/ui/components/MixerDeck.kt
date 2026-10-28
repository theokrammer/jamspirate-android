package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.StudioUiState
import kotlin.math.roundToInt

@Composable
fun MixerDeck(
    uiState: StudioUiState,
    onSoloVolumeChange: (Float) -> Unit,
    onBackingVolumeChange: (Float) -> Unit,
    onMasterVolumeChange: (Float) -> Unit,
    onCrossfaderChange: (Float) -> Unit,
    onToggleSoloMute: () -> Unit,
    onToggleBackingMute: () -> Unit,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mixer_deck_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF38BDF8).copy(alpha = 0.35f), Color(0xFF0F172A))
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Expandable Header Bar / Dropdown Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("button_toggle_mixer_dropdown"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Mixer Console",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "MIXING CONSOLE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "MASTER ${(uiState.masterVolume * 100).roundToInt()}%",
                                    color = Color(0xFF10B981),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = "Lead ${(uiState.soloVolume * 100).roundToInt()}% • Backing ${(uiState.backingVolume * 100).roundToInt()}% • ${if (uiState.crossfaderBalance < 0.45f) "Backing Focus" else if (uiState.crossfaderBalance > 0.55f) "Lead Focus" else "Balanced"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isExpanded) {
                        // Balance Reset Pill Button
                        OutlinedButton(
                            onClick = { onCrossfaderChange(0.5f) },
                            modifier = Modifier
                                .height(26.dp)
                                .testTag("button_reset_balance"),
                            contentPadding = PaddingValues(horizontal = 7.dp, vertical = 0.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF38BDF8)
                            ),
                            border = ButtonDefaults.outlinedButtonBorder().copy(
                                brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF818CF8)))
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Balance,
                                contentDescription = "Center",
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Center",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Mixer" else "Expand Mixer",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Dropdown Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))

            // Main 2-Channel Strips Row (Improv Lead on Left, Backing on Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // CHANNEL 1: IMPROV LEAD
                MixerChannelStrip(
                    channelTitle = "CH 1 • IMPROV",
                    subtitle = uiState.activeInstrument.displayName,
                    accentColor = Color(0xFFF59E0B),
                    volume = uiState.soloVolume,
                    isMuted = uiState.isSoloMuted,
                    isActiveSignal = uiState.isJamming && uiState.recentNotes.isNotEmpty(),
                    onVolumeChange = onSoloVolumeChange,
                    onMuteToggle = onToggleSoloMute,
                    muteTestTag = "button_mute_improv",
                    sliderTestTag = "slider_improv_volume",
                    modifier = Modifier.weight(1f)
                )

                // CHANNEL 2: BACKING TRACK
                MixerChannelStrip(
                    channelTitle = "CH 2 • BACKING",
                    subtitle = uiState.activeTrack.title,
                    accentColor = Color(0xFF06B6D4),
                    volume = uiState.backingVolume,
                    isMuted = uiState.isBackingMuted,
                    isActiveSignal = uiState.isJamming && !uiState.isBackingMuted,
                    onVolumeChange = onBackingVolumeChange,
                    onMuteToggle = onToggleBackingMute,
                    muteTestTag = "button_mute_backing",
                    sliderTestTag = "slider_backing_volume",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // QUICK MIX CROSSFADER SECTION
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E2433),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "◄ BACKING",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.crossfaderBalance < 0.45f) Color(0xFF06B6D4) else Color(0xFF64748B)
                        )
                        Text(
                            text = "CROSSFADER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "IMPROV ►",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.crossfaderBalance > 0.55f) Color(0xFFF59E0B) else Color(0xFF64748B)
                        )
                    }

                    Slider(
                        value = uiState.crossfaderBalance,
                        onValueChange = onCrossfaderChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp)
                            .testTag("slider_crossfader_balance"),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val backingPercent = when {
                            uiState.crossfaderBalance <= 0.5f -> 100
                            else -> ((1.0f - uiState.crossfaderBalance) * 200).roundToInt()
                        }
                        val improvPercent = when {
                            uiState.crossfaderBalance >= 0.5f -> 100
                            else -> (uiState.crossfaderBalance * 200).roundToInt()
                        }
                        Text(
                            text = "Backing: $backingPercent%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = if (uiState.crossfaderBalance in 0.48f..0.52f) "Centered" else "Split",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "Improv: $improvPercent%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MASTER OUTPUT FADER
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Master Volume",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "MASTER",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Slider(
                    value = uiState.masterVolume,
                    onValueChange = onMasterVolumeChange,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("slider_master_volume"),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF10B981),
                        activeTrackColor = Color(0xFF10B981),
                        inactiveTrackColor = Color(0xFF334155)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${(uiState.masterVolume * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.width(42.dp)
                )
            }
        }
    }
}
}
}

@Composable
private fun MixerChannelStrip(
    channelTitle: String,
    subtitle: String,
    accentColor: Color,
    volume: Float,
    isMuted: Boolean,
    isActiveSignal: Boolean,
    onVolumeChange: (Float) -> Unit,
    onMuteToggle: () -> Unit,
    muteTestTag: String,
    sliderTestTag: String,
    modifier: Modifier = Modifier
) {
    val muteBgColor by animateColorAsState(
        targetValue = if (isMuted) Color(0xFFEF4444) else Color(0xFF334155),
        label = "muteColor"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A2234),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isMuted) Color(0xFFEF4444).copy(alpha = 0.5f) else Color(0xFF2E3B52)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Channel Header & VU Activity Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = channelTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontSize = 10.sp
                )

                // Simulated Signal LED Meter
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val ledActive = isActiveSignal && !isMuted
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (ledActive) Color(0xFF22C55E) else Color(0xFF1E293B))
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (ledActive && volume > 0.4f) Color(0xFFF59E0B) else Color(0xFF1E293B))
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (ledActive && volume > 0.85f) Color(0xFFEF4444) else Color(0xFF1E293B))
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFCBD5E1),
                maxLines = 1,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Volume Percentage readout
            Text(
                text = if (isMuted) "MUTED" else "${(volume * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isMuted) Color(0xFFEF4444) else Color.White
            )

            // Volume Slider
            Slider(
                value = volume,
                onValueChange = onVolumeChange,
                enabled = !isMuted,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(sliderTestTag),
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = Color(0xFF334155),
                    disabledThumbColor = Color(0xFF64748B),
                    disabledActiveTrackColor = Color(0xFF475569)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // MUTE BUTTON
            Button(
                onClick = onMuteToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag(muteTestTag),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = muteBgColor,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isMuted) "MUTED" else "MUTE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
