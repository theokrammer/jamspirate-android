package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AudioOutputDestination
import com.example.ai.LookaheadNote
import com.example.engine.MusicTheory

enum class NotationViewMode(val title: String) {
    COMPARE_BOTH("Both"),
    LOCAL_ONLY("Local"),
    CLOUD_ONLY("Cloud")
}

/**
 * Standard 5-Line Musical Staff Notation Component
 * Renders batched 2 bars of improvisation phrases for visual comparison between
 * Local Algorithmic Theory and Cloud AI (Google Gemini 3.8 Flash).
 */
@Composable
fun MusicNotationComparisonCard(
    localNotes: List<LookaheadNote>,
    cloudNotes: List<LookaheadNote>,
    barRange: Pair<Int, Int>,
    chordNames: Pair<String, String>,
    currentBar: Int,
    currentSub: Int,
    isJamming: Boolean,
    audioDestination: AudioOutputDestination,
    onSelectAudioDestination: (AudioOutputDestination) -> Unit,
    onRefreshCloudBatch: () -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isLocalBackupEnabled: Boolean = true,
    onToggleLocalBackup: (Boolean) -> Unit = {},
    isLlmFetching: Boolean = false,
    hasApiKey: Boolean = false,
    activeModelName: String = "gemini-2.5-flash",
    isFamousPieceMode: Boolean = false,
    famousPieceName: String = "",
    customPromptDirective: String = "",
    onOpenAiPromptSettings: () -> Unit = {},
    onOpenComparisonMatrix: () -> Unit = {},
    timeSignature: String = "4/4",
    beatsPerBar: Int = 4,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(NotationViewMode.COMPARE_BOTH) }
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_music_notation"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.5f),
                    Color(0xFFF59E0B).copy(alpha = 0.5f)
                )
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Expandable Header Bar / Dropdown Trigger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 12.dp, vertical = 9.dp)
                    .testTag("button_toggle_notation_dropdown"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF0284C7), Color(0xFFD97706))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Standard Sheet Music",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "SHEET MUSIC",
                                color = Color(0xFFF8FAFC),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "2 BARS • $timeSignature",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Bars ${barRange.first}-${barRange.second} (${chordNames.first.ifBlank { "Bar 1" }} • ${chordNames.second.ifBlank { "Bar 2" }})",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Small badge showing current audio output
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (audioDestination == AudioOutputDestination.LOCAL)
                                    Color(0xFF0369A1).copy(alpha = 0.35f)
                                else
                                    Color(0xFFB45309).copy(alpha = 0.35f)
                            )
                            .border(
                                1.dp,
                                if (audioDestination == AudioOutputDestination.LOCAL) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                                RoundedCornerShape(5.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (audioDestination == AudioOutputDestination.LOCAL) "LOCAL" else "CLOUD",
                            color = if (audioDestination == AudioOutputDestination.LOCAL) Color(0xFF38BDF8) else Color(0xFFFBBF24),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Compare Matrix / PDF Button in header
                    Surface(
                        onClick = onOpenComparisonMatrix,
                        shape = RoundedCornerShape(5.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.25f),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = SolidColor(Color(0xFF38BDF8))
                        ),
                        modifier = Modifier.testTag("button_open_comparison_matrix_header")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(11.dp))
                            Text(
                                text = "COMPARE",
                                color = Color(0xFF38BDF8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse Notation" else "Expand Notation",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Expandable Notation Panel Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    // Controls Row: View Mode Tabs & Audio Output Routing Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // View mode segmented tabs
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            NotationViewMode.entries.forEach { mode ->
                                val isSelected = viewMode == mode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) Color(0xFF334155) else Color.Transparent)
                                        .clickable { viewMode = mode }
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                        .testTag("tab_notation_${mode.name.lowercase()}")
                                ) {
                                    Text(
                                        text = mode.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFFF8FAFC) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                onClick = onOpenComparisonMatrix,
                                shape = RoundedCornerShape(5.dp),
                                color = Color(0xFF0369A1),
                                modifier = Modifier.testTag("button_open_matrix_compare")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text("Compare Matrix / PDF", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Customize AI Prompt Button
                            IconButton(
                                onClick = onOpenAiPromptSettings,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("button_open_prompt_settings")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Customize AI Improv Prompt",
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Refresh / Re-query Cloud Batch Button
                            IconButton(
                                onClick = onRefreshCloudBatch,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("button_refresh_cloud_batch")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Regenerate Cloud Batch",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Audio Output Destination Banner (Selecting cloud changes audio output)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF131D33),
                        border = CardDefaults.outlinedCardBorder().copy(
                            width = 1.dp,
                            brush = SolidColor(Color(0xFF1E293B))
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = if (audioDestination == AudioOutputDestination.LOCAL) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "AUDIO:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ButtonPill(
                                    text = "Local Theory",
                                    selected = audioDestination == AudioOutputDestination.LOCAL,
                                    activeColor = Color(0xFF38BDF8),
                                    onClick = { onSelectAudioDestination(AudioOutputDestination.LOCAL) },
                                    tag = "button_route_local",
                                    modifier = Modifier.weight(1f)
                                )
                                ButtonPill(
                                    text = "Cloud AI",
                                    selected = audioDestination == AudioOutputDestination.CLOUD,
                                    activeColor = Color(0xFFF59E0B),
                                    onClick = { onSelectAudioDestination(AudioOutputDestination.CLOUD) },
                                    tag = "button_route_cloud",
                                    modifier = Modifier.weight(1f)
                                )
                                // Local Engine Backup Toggle Button
                                Surface(
                                    onClick = { onToggleLocalBackup(!isLocalBackupEnabled) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isLocalBackupEnabled) Color(0xFF10B981).copy(alpha = 0.18f) else Color(0xFFF43F5E).copy(alpha = 0.18f),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = 1.dp,
                                        brush = SolidColor(if (isLocalBackupEnabled) Color(0xFF10B981) else Color(0xFFF43F5E))
                                    ),
                                    modifier = Modifier.testTag("button_toggle_backup_inline")
                                ) {
                                    Text(
                                        text = if (isLocalBackupEnabled) "Backup: ON" else "Backup: OFF",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLocalBackupEnabled) Color(0xFF10B981) else Color(0xFFF43F5E),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val cloudTitle = if (hasApiKey) "Cloud AI ($activeModelName)" else "Cloud AI (Neural Solo Model)"
                    val modeTag = if (isFamousPieceMode && famousPieceName.isNotBlank()) " • 🎵 \"$famousPieceName\"" else ""
                    val promptTag = if (customPromptDirective.isNotBlank()) " • [\"${customPromptDirective.take(22)}...\"]" else modeTag
                    val cloudSubtitle = when {
                        isLlmFetching -> "Querying $activeModelName live..."
                        cloudNotes.isNotEmpty() -> "${cloudNotes.size} notes • ${if (hasApiKey) "Live Lookahead" else "Neural phrasing"}$promptTag"
                        else -> "Ready • Tap 🔄 to generate"
                    }

                    // Real-Time Playback Indicator Banner
                    if (isJamming) {
                        val activeNotes = if (audioDestination == AudioOutputDestination.LOCAL) localNotes else cloudNotes
                        val activeNote = activeNotes.firstOrNull { note ->
                            note.barIndex == currentBar && currentSub >= note.subInBar && currentSub < (note.subInBar + note.durationSubdivisions.coerceAtLeast(1))
                        }
                        val activeNoteName = activeNote?.let { MusicTheory.midiToNoteName(it.midiPitch) }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0C192E),
                            border = CardDefaults.outlinedCardBorder().copy(
                                width = 1.dp,
                                brush = SolidColor(if (activeNote != null) Color(0xFFF59E0B) else Color(0xFF1E293B))
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text(
                                        text = "MEASURE: Bar ${currentBar + 1}  •  BEAT ${(currentSub / 4) + 1}.${(currentSub % 4) + 1} ($timeSignature)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF1F5F9),
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                // Fixed-size active note badge to prevent layout jittering and size jumping
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (activeNote != null) Color(0xFFF59E0B).copy(alpha = 0.25f) else Color(0xFF1E293B).copy(alpha = 0.35f),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        width = 1.dp,
                                        brush = SolidColor(if (activeNote != null) Color(0xFFFBBF24) else Color(0xFF334155))
                                    ),
                                    modifier = Modifier
                                        .width(135.dp)
                                        .height(24.dp)
                                        .testTag("panel_sounding_note")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (activeNote != null) "▶ SOUNDING: $activeNoteName" else "♫ Rest",
                                            fontSize = 10.sp,
                                            fontWeight = if (activeNote != null) FontWeight.Black else FontWeight.Medium,
                                            color = if (activeNote != null) Color(0xFFFBBF24) else Color(0xFF64748B),
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Standard Music Notation Staves
                    when (viewMode) {
                        NotationViewMode.COMPARE_BOTH -> {
                            // Top Staff: Local Theory Engine
                            StaffLabelHeader(
                                title = "Local Theory Engine",
                                subtitle = "${localNotes.size} notes • Diatonic target tones",
                                badgeText = if (audioDestination == AudioOutputDestination.LOCAL) "AUDIO ON" else "MUTED",
                                badgeColor = if (audioDestination == AudioOutputDestination.LOCAL) Color(0xFF10B981) else Color(0xFF64748B),
                                titleColor = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StandardStaffCanvas(
                                notes = localNotes,
                                bar1 = barRange.first,
                                bar2 = barRange.second,
                                chord1 = chordNames.first,
                                chord2 = chordNames.second,
                                noteColor = Color(0xFF38BDF8),
                                currentBar = currentBar,
                                currentSub = currentSub,
                                isJamming = isJamming,
                                textMeasurer = textMeasurer,
                                timeSignature = timeSignature,
                                beatsPerBar = beatsPerBar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Bottom Staff: Cloud AI (Gemini 3.8 Flash)
                            StaffLabelHeader(
                                title = cloudTitle,
                                subtitle = cloudSubtitle,
                                badgeText = if (audioDestination == AudioOutputDestination.CLOUD) "AUDIO ON" else "MUTED",
                                badgeColor = if (audioDestination == AudioOutputDestination.CLOUD) Color(0xFFF59E0B) else Color(0xFF64748B),
                                titleColor = Color(0xFFFBBF24)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StandardStaffCanvas(
                                notes = cloudNotes,
                                bar1 = barRange.first,
                                bar2 = barRange.second,
                                chord1 = chordNames.first,
                                chord2 = chordNames.second,
                                noteColor = Color(0xFFFBBF24),
                                currentBar = currentBar,
                                currentSub = currentSub,
                                isJamming = isJamming,
                                textMeasurer = textMeasurer,
                                timeSignature = timeSignature,
                                beatsPerBar = beatsPerBar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(104.dp)
                            )
                        }

                        NotationViewMode.LOCAL_ONLY -> {
                            StaffLabelHeader(
                                title = "Local Theory Engine (2 Bars)",
                                subtitle = "${localNotes.size} notes • Diatonic scale phrases",
                                badgeText = if (audioDestination == AudioOutputDestination.LOCAL) "AUDIO ON" else "MUTED",
                                badgeColor = if (audioDestination == AudioOutputDestination.LOCAL) Color(0xFF10B981) else Color(0xFF64748B),
                                titleColor = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StandardStaffCanvas(
                                notes = localNotes,
                                bar1 = barRange.first,
                                bar2 = barRange.second,
                                chord1 = chordNames.first,
                                chord2 = chordNames.second,
                                noteColor = Color(0xFF38BDF8),
                                currentBar = currentBar,
                                currentSub = currentSub,
                                isJamming = isJamming,
                                textMeasurer = textMeasurer,
                                timeSignature = timeSignature,
                                beatsPerBar = beatsPerBar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(112.dp)
                            )
                        }

                        NotationViewMode.CLOUD_ONLY -> {
                            StaffLabelHeader(
                                title = "$cloudTitle (2 Bars)",
                                subtitle = cloudSubtitle,
                                badgeText = if (audioDestination == AudioOutputDestination.CLOUD) "AUDIO ON" else "MUTED",
                                badgeColor = if (audioDestination == AudioOutputDestination.CLOUD) Color(0xFFF59E0B) else Color(0xFF64748B),
                                titleColor = Color(0xFFFBBF24)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            StandardStaffCanvas(
                                notes = cloudNotes,
                                bar1 = barRange.first,
                                bar2 = barRange.second,
                                chord1 = chordNames.first,
                                chord2 = chordNames.second,
                                noteColor = Color(0xFFFBBF24),
                                currentBar = currentBar,
                                currentSub = currentSub,
                                isJamming = isJamming,
                                textMeasurer = textMeasurer,
                                timeSignature = timeSignature,
                                beatsPerBar = beatsPerBar,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(112.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Comparison Metrics Strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF0A0F1D))
                            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LOCAL THEORY",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${localNotes.size} notes • Diatonic target tones",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(18.dp)
                                .background(Color(0xFF1E293B))
                        )

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (hasApiKey) "CLOUD AI (${activeModelName.uppercase()})" else "CLOUD AI (NEURAL SOLO MODEL)",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = when {
                                    isLlmFetching -> "⚡ Generating with Gemini..."
                                    cloudNotes.isNotEmpty() -> "${cloudNotes.size} notes • ${if (hasApiKey) "Live Lookahead" else "Neural Phrasing (Demo)"}"
                                    else -> "Ready (Tap 🔄)"
                                },
                                fontSize = 10.sp,
                                color = if (isLlmFetching) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ButtonPill(
    text: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (selected) activeColor.copy(alpha = 0.25f) else Color(0xFF1E293B),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = SolidColor(if (selected) activeColor else Color(0xFF334155))
        ),
        modifier = modifier.testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (selected) activeColor else Color(0xFF64748B))
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) activeColor else Color(0xFFCBD5E1),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun StaffLabelHeader(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    titleColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(titleColor)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(badgeColor.copy(alpha = 0.20f))
                .border(1.dp, badgeColor.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
        ) {
            Text(
                text = badgeText,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                fontFamily = FontFamily.Monospace,
                maxLines = 1
            )
        }
    }
    if (subtitle.isNotBlank()) {
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Custom Canvas drawing a standard 5-line musical staff with:
 * - Treble Clef symbol (𝄞) and 4/4 Time Signature
 * - 5 horizontal staff lines (E4 to F5)
 * - Bar separator dividing Measure 1 and Measure 2, plus end double bar line
 * - Chord names badge atop each measure
 * - Accurately positioned note heads on staff lines / spaces
 * - Note stems with up/down direction and flags for 16th/8th notes
 * - Ledger lines for notes below E4 (C4 Middle C, A3) or above F5 (A5, C6)
 * - Sharp accidentals (♯)
 * - Real-time animated playhead cursor tracking current playback tick!
 */
@Composable
private fun StandardStaffCanvas(
    notes: List<LookaheadNote>,
    bar1: Int,
    bar2: Int,
    chord1: String,
    chord2: String,
    noteColor: Color,
    currentBar: Int,
    currentSub: Int,
    isJamming: Boolean,
    textMeasurer: TextMeasurer,
    timeSignature: String = "4/4",
    beatsPerBar: Int = 4,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF070B14))
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(10.dp))
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Staff geometry
        val staffStartX = 52.dp.toPx()
        val staffEndX = canvasWidth - 12.dp.toPx()
        val totalScoreWidth = (staffEndX - staffStartX).coerceAtLeast(100f)
        val measureWidth = totalScoreWidth / 2f
        val lineSpacing = 8.dp.toPx()
        val staffCenterY = canvasHeight / 2f // Middle line B4 (Line 3)

        // 5 Staff Lines Y coordinates:
        // Line 5 (F5): -2 * lineSpacing
        // Line 4 (D5): -1 * lineSpacing
        // Line 3 (B4):  0 * lineSpacing (staffCenterY)
        // Line 2 (G4): +1 * lineSpacing
        // Line 1 (E4): +2 * lineSpacing
        val staffLineColor = Color(0xFF334155)
        for (i in -2..2) {
            val y = staffCenterY + i * lineSpacing
            drawLine(
                color = staffLineColor,
                start = Offset(staffStartX, y),
                end = Offset(staffEndX, y),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        // Draw Measure Bar Lines:
        // 1. Start bar line
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(staffStartX, staffCenterY - 2 * lineSpacing),
            end = Offset(staffStartX, staffCenterY + 2 * lineSpacing),
            strokeWidth = 1.5.dp.toPx()
        )
        // 2. Middle bar line (between Bar 1 and Bar 2)
        val midBarX = staffStartX + measureWidth
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(midBarX, staffCenterY - 2 * lineSpacing),
            end = Offset(midBarX, staffCenterY + 2 * lineSpacing),
            strokeWidth = 2.dp.toPx()
        )
        // 3. End double bar line
        drawLine(
            color = Color(0xFF64748B),
            start = Offset(staffEndX - 4.dp.toPx(), staffCenterY - 2 * lineSpacing),
            end = Offset(staffEndX - 4.dp.toPx(), staffCenterY + 2 * lineSpacing),
            strokeWidth = 1.2.dp.toPx()
        )
        drawLine(
            color = Color(0xFF94A3B8),
            start = Offset(staffEndX, staffCenterY - 2 * lineSpacing),
            end = Offset(staffEndX, staffCenterY + 2 * lineSpacing),
            strokeWidth = 2.5.dp.toPx()
        )

        val subdivisionsPerBar = (beatsPerBar * 4).coerceAtLeast(4)
        val timeSigParts = timeSignature.split("/")
        val timeSigTop = timeSigParts.getOrNull(0)?.trim()?.ifEmpty { null } ?: "$beatsPerBar"
        val timeSigBottom = timeSigParts.getOrNull(1)?.trim()?.ifEmpty { null } ?: "4"

        // Beat subdivision tick marks along the bottom staff line
        for (b in 1 until beatsPerBar) {
            val tickFraction = b.toFloat() / beatsPerBar.toFloat()
            val tickX1 = staffStartX + tickFraction * measureWidth
            val tickX2 = midBarX + tickFraction * measureWidth
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(tickX1, staffCenterY - 2 * lineSpacing),
                end = Offset(tickX1, staffCenterY + 2 * lineSpacing),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = Offset(tickX2, staffCenterY - 2 * lineSpacing),
                end = Offset(tickX2, staffCenterY + 2 * lineSpacing),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
            )
        }

        // Draw Treble Clef (𝄞) and Time Signature safely
        drawSafeText(
            textMeasurer = textMeasurer,
            text = "𝄞",
            x = 10.dp.toPx(),
            y = staffCenterY - 26.dp.toPx(),
            style = TextStyle(
                fontSize = 34.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )
        )
        drawSafeText(
            textMeasurer = textMeasurer,
            text = "$timeSigTop\n$timeSigBottom",
            x = 36.dp.toPx(),
            y = staffCenterY - 14.dp.toPx(),
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        )

        // Draw Measure Headers / Chord Badges above each measure
        val pageStartBar = bar1 - 1
        val isBar1Active = isJamming && (currentBar == pageStartBar)
        val isBar2Active = isJamming && (currentBar == pageStartBar + 1)
        val chord1Text = "${if (isBar1Active) "▶ " else ""}Bar $bar1: ${if (chord1.isNotBlank()) chord1 else "Am"}"
        val chord2Text = "${if (isBar2Active) "▶ " else ""}Bar $bar2: ${if (chord2.isNotBlank()) chord2 else "Dm"}"
        drawSafeText(
            textMeasurer = textMeasurer,
            text = chord1Text,
            x = staffStartX + 6.dp.toPx(),
            y = 4.dp.toPx(),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = if (isBar1Active) FontWeight.Black else FontWeight.Bold,
                color = if (isBar1Active) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        )
        drawSafeText(
            textMeasurer = textMeasurer,
            text = chord2Text,
            x = midBarX + 6.dp.toPx(),
            y = 4.dp.toPx(),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = if (isBar2Active) FontWeight.Black else FontWeight.Bold,
                color = if (isBar2Active) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        )

        // Render Planned Notes on the Staff
        if (notes.isEmpty()) {
            drawSafeText(
                textMeasurer = textMeasurer,
                text = "Rest / No notes queued for this phrase",
                x = staffStartX + 20.dp.toPx(),
                y = staffCenterY - 8.dp.toPx(),
                style = TextStyle(
                    fontSize = 11.sp,
                    color = Color(0xFF475569),
                    fontFamily = FontFamily.Monospace
                )
            )
        } else {
            for (note in notes) {
                val barOffsetIndex = note.barIndex - pageStartBar
                if (barOffsetIndex !in 0..1) continue

                val noteX = staffStartX + (barOffsetIndex * measureWidth) +
                        ((note.subInBar.coerceIn(0, subdivisionsPerBar - 1) + 0.5f) / subdivisionsPerBar.toFloat()) * (measureWidth - 12.dp.toPx()) + 4.dp.toPx()

                val pitchClass = note.midiPitch % 12
                val octave = (note.midiPitch / 12) - 1
                val diatonicInOct = when (pitchClass) {
                    0 -> 0 // C
                    1 -> 0 // C#
                    2 -> 1 // D
                    3 -> 1 // D#
                    4 -> 2 // E
                    5 -> 3 // F
                    6 -> 3 // F#
                    7 -> 4 // G
                    8 -> 4 // G#
                    9 -> 5 // A
                    10 -> 5 // A#
                    11 -> 6 // B
                    else -> 0
                }
                val isSharp = pitchClass in listOf(1, 3, 6, 8, 10)
                val totalDiatonicStep = (octave * 7) + diatonicInOct
                // B4 (Line 3 center) has totalDiatonicStep = 4*7 + 6 = 34
                val stepFromB4 = totalDiatonicStep - 34

                // Note Y coordinate: each step is half a line spacing
                val noteY = staffCenterY - (stepFromB4 * (lineSpacing / 2f))

                // Check if this note is currently sounding in real-time
                val isActiveNote = isJamming &&
                        (note.barIndex == currentBar) &&
                        (currentSub >= note.subInBar) &&
                        (currentSub < (note.subInBar + note.durationSubdivisions.coerceAtLeast(1)))

                // Draw luminous radial aura around actively sounding note
                if (isActiveNote) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFBBF24).copy(alpha = 0.85f),
                                Color(0xFFF59E0B).copy(alpha = 0.45f),
                                Color(0xFFD97706).copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            center = Offset(noteX, noteY),
                            radius = 22.dp.toPx()
                        ),
                        center = Offset(noteX, noteY),
                        radius = 22.dp.toPx()
                    )
                }

                // Draw Ledger Lines if needed:
                val ledgerWidth = 14.dp.toPx()
                if (stepFromB4 <= -6) {
                    val ly = staffCenterY - (-6 * (lineSpacing / 2f))
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(noteX - ledgerWidth / 2f, ly),
                        end = Offset(noteX + ledgerWidth / 2f, ly),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
                if (stepFromB4 <= -8) {
                    val ly = staffCenterY - (-8 * (lineSpacing / 2f))
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(noteX - ledgerWidth / 2f, ly),
                        end = Offset(noteX + ledgerWidth / 2f, ly),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
                if (stepFromB4 >= 6) {
                    val ly = staffCenterY - (6 * (lineSpacing / 2f))
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(noteX - ledgerWidth / 2f, ly),
                        end = Offset(noteX + ledgerWidth / 2f, ly),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }
                if (stepFromB4 >= 8) {
                    val ly = staffCenterY - (8 * (lineSpacing / 2f))
                    drawLine(
                        color = Color(0xFF64748B),
                        start = Offset(noteX - ledgerWidth / 2f, ly),
                        end = Offset(noteX + ledgerWidth / 2f, ly),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }

                // Draw Sharp Accidental (♯) if chromatic
                if (isSharp) {
                    drawSafeText(
                        textMeasurer = textMeasurer,
                        text = "♯",
                        x = noteX - 10.dp.toPx(),
                        y = noteY - 8.dp.toPx(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActiveNote) Color(0xFFFBBF24) else noteColor
                        )
                    )
                }

                // Draw Elliptical Note Head (rotated -20 degrees)
                val headW = 9.dp.toPx()
                val headH = 6.5.dp.toPx()
                val headFillColor = if (isActiveNote) Color(0xFFFFFBEB) else noteColor
                rotate(degrees = -20f, pivot = Offset(noteX, noteY)) {
                    drawOval(
                        color = headFillColor,
                        topLeft = Offset(noteX - headW / 2f, noteY - headH / 2f),
                        size = Size(headW, headH)
                    )
                    if (isActiveNote) {
                        drawOval(
                            color = Color(0xFFF59E0B),
                            topLeft = Offset(noteX - (headW + 4.dp.toPx()) / 2f, noteY - (headH + 4.dp.toPx()) / 2f),
                            size = Size(headW + 4.dp.toPx(), headH + 4.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }

                // Draw Note Stem & Flags
                val stemHeight = 22.dp.toPx()
                val stemUp = stepFromB4 < 0
                val stemX = if (stemUp) noteX + (headW / 2f) - 0.5.dp.toPx() else noteX - (headW / 2f) + 0.5.dp.toPx()
                val stemEndY = if (stemUp) noteY - stemHeight else noteY + stemHeight

                drawLine(
                    color = if (isActiveNote) Color(0xFFFBBF24) else noteColor,
                    start = Offset(stemX, noteY),
                    end = Offset(stemX, stemEndY),
                    strokeWidth = if (isActiveNote) 1.8.dp.toPx() else 1.4.dp.toPx()
                )

                // Flags for 16th notes (dur <= 1) and 8th notes (dur == 2)
                if (note.durationSubdivisions <= 1) {
                    // Double flag (16th note)
                    val flagDir = if (stemUp) 1f else -1f
                    drawLine(
                        color = if (isActiveNote) Color(0xFFFBBF24) else noteColor,
                        start = Offset(stemX, stemEndY),
                        end = Offset(stemX + 5.dp.toPx(), stemEndY + 4.dp.toPx() * flagDir),
                        strokeWidth = 1.4.dp.toPx()
                    )
                    drawLine(
                        color = if (isActiveNote) Color(0xFFFBBF24) else noteColor,
                        start = Offset(stemX, stemEndY + 3.dp.toPx() * flagDir),
                        end = Offset(stemX + 5.dp.toPx(), stemEndY + 7.dp.toPx() * flagDir),
                        strokeWidth = 1.4.dp.toPx()
                    )
                } else if (note.durationSubdivisions <= 2) {
                    // Single flag (8th note)
                    val flagDir = if (stemUp) 1f else -1f
                    drawLine(
                        color = if (isActiveNote) Color(0xFFFBBF24) else noteColor,
                        start = Offset(stemX, stemEndY),
                        end = Offset(stemX + 5.dp.toPx(), stemEndY + 4.dp.toPx() * flagDir),
                        strokeWidth = 1.4.dp.toPx()
                    )
                }

                // Tiny Note Name Text (e.g. "C4", "G5") beneath or above note for effortless reading
                val noteName = MusicTheory.midiToNoteName(note.midiPitch)
                val textY = if (stemUp) noteY + 5.dp.toPx() else noteY - 14.dp.toPx()
                drawSafeText(
                    textMeasurer = textMeasurer,
                    text = if (isActiveNote) "▶ $noteName" else noteName,
                    x = noteX - (if (isActiveNote) 12.dp.toPx() else 8.dp.toPx()),
                    y = textY,
                    style = TextStyle(
                        fontSize = if (isActiveNote) 9.sp else 8.sp,
                        fontWeight = if (isActiveNote) FontWeight.Black else FontWeight.Bold,
                        color = if (isActiveNote) Color(0xFFFBBF24) else noteColor.copy(alpha = 0.85f),
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }

        // Real-Time Playback Playhead Cursor
        if (isJamming) {
            val currentOffsetBar = (currentBar - pageStartBar).coerceIn(0, 1)
            val playheadX = staffStartX + (currentOffsetBar * measureWidth) +
                    ((currentSub.coerceIn(0, subdivisionsPerBar - 1) + 0.5f) / subdivisionsPerBar.toFloat()) * (measureWidth - 12.dp.toPx()) + 4.dp.toPx()

            // Glowing outer beam halo
            drawLine(
                color = Color(0xFFEF4444).copy(alpha = 0.35f),
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, canvasHeight),
                strokeWidth = 6.dp.toPx()
            )
            // Core laser line
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFFEF4444))
                ),
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, canvasHeight),
                strokeWidth = 2.5.dp.toPx()
            )
            // Cursor top indicator triangle
            val topPath = Path().apply {
                moveTo(playheadX - 5.dp.toPx(), 0f)
                lineTo(playheadX + 5.dp.toPx(), 0f)
                lineTo(playheadX, 8.dp.toPx())
                close()
            }
            drawPath(path = topPath, color = Color(0xFFEF4444))

            // Cursor foot indicator triangle
            val footPath = Path().apply {
                moveTo(playheadX - 5.dp.toPx(), canvasHeight)
                lineTo(playheadX + 5.dp.toPx(), canvasHeight)
                lineTo(playheadX, canvasHeight - 8.dp.toPx())
                close()
            }
            drawPath(path = footPath, color = Color(0xFFEF4444))
        }
    }
}

/**
 * Safely measures and renders text onto the DrawScope canvas using pre-measured TextLayoutResult,
 * preventing runtime [IllegalArgumentException] from negative constraints when coordinates are near
 * or outside canvas boundaries.
 */
private fun DrawScope.drawSafeText(
    textMeasurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    style: TextStyle
) {
    if (text.isEmpty() || size.width <= 0 || size.height <= 0) return
    val layout = textMeasurer.measure(
        text = text,
        style = style
    )
    val textW = layout.size.width.toFloat()
    val textH = layout.size.height.toFloat()
    val maxX = (size.width - textW).coerceAtLeast(0f)
    val maxY = (size.height - textH).coerceAtLeast(0f)
    val targetX = x.coerceIn(0f, maxX)
    val targetY = y.coerceIn(0f, maxY)

    drawText(
        textLayoutResult = layout,
        topLeft = Offset(targetX, targetY)
    )
}
