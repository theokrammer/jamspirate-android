package com.example.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.ui.draw.rotate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ai.AudioOutputDestination
import com.example.ui.components.AiSettingsDialog
import com.example.ui.components.BackingDeck
import com.example.ui.components.InstrumentDeck
import com.example.ui.components.MixerDeck
import com.example.ui.components.MusicNotationComparisonCard
import com.example.ui.components.MusicianInfluenceDeck
import com.example.ui.components.RealTimeAudioMonitor
import com.example.ui.comparison.ComparisonMatrixDialog
import com.example.ui.comparison.ComparisonGalleryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    viewModel: StudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var showAiSettingsDialog by remember { mutableStateOf(false) }

    var isImprovExpanded by remember { mutableStateOf(true) }
    var isBackingExpanded by remember { mutableStateOf(false) }
    var isMusicianExpanded by remember { mutableStateOf(false) }
    var isInstrumentExpanded by remember { mutableStateOf(false) }

    // Permission launcher for microphone recording
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val started = viewModel.startMicRecording()
            if (!started) {
                Toast.makeText(context, "Failed to initialize microphone", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Microphone permission required for live looping", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("studio_scaffold"),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF22D3EE), Color(0xFFF59E0B))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Jamspirate!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (uiState.isJamming) Color(0xFF10B981) else Color(0xFF334155))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (uiState.isJamming) "LIVE" else "READY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "Real-Time AI Improviser",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Cloud LLM Lookahead Settings Button
                    FilledTonalIconButton(
                        onClick = { showAiSettingsDialog = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (uiState.isLookaheadBatchingEnabled && uiState.activeLlmProvider != com.example.ai.LlmProvider.LOCAL_THEORY) {
                                Color(0xFF8B5CF6).copy(alpha = 0.35f)
                            } else {
                                Color(0xFF1E293B)
                            }
                        ),
                        modifier = Modifier.size(40.dp).testTag("app_bar_cloud_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Cloud LLM Settings",
                            tint = if (uiState.isLookaheadBatchingEnabled && uiState.activeLlmProvider != com.example.ai.LlmProvider.LOCAL_THEORY) {
                                Color(0xFFA78BFA)
                            } else {
                                Color(0xFF94A3B8)
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Header Improv Track Mute / Unmute Button (Replaces AI Prompt Icon)
                    FilledTonalIconButton(
                        onClick = { viewModel.toggleSoloMute() },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = if (uiState.isSoloMuted) Color(0xFFEF4444).copy(alpha = 0.25f) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("app_bar_mute_solo_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isSoloMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = if (uiState.isSoloMuted) "Unmute Improv Track" else "Mute Improv Track",
                            tint = if (uiState.isSoloMuted) Color(0xFFF87171) else Color(0xFF22D3EE),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Jam Start / Stop Button
                    IconButton(
                        onClick = { viewModel.toggleJam() },
                        modifier = Modifier.size(40.dp).testTag("app_bar_jam_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.isJamming) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Toggle Jam",
                            tint = if (uiState.isJamming) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0F17)
                )
            )
        },
        containerColor = Color(0xFF0B0F17)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. SECTION: "Improv"
            CollapsibleSection(
                title = "Improv",
                subtitle = "${uiState.detectedNoteName} (${uiState.detectedKey} ${uiState.detectedScale.displayName.substringBefore(" ")})",
                badgeText = if (uiState.isJamming) "LIVE" else "READY",
                badgeColor = if (uiState.isJamming) Color(0xFF10B981) else Color(0xFF38BDF8),
                isExpanded = isImprovExpanded,
                onToggle = { isImprovExpanded = !isImprovExpanded },
                testTag = "section_improv"
            ) {
                RealTimeAudioMonitor(
                    pitchHz = uiState.detectedPitchHz,
                    noteName = uiState.detectedNoteName,
                    cents = uiState.detectedCents,
                    confidence = uiState.detectedConfidence,
                    bpm = uiState.detectedBpm,
                    timeSignature = uiState.timeSignature,
                    beatPulse = uiState.beatPulse,
                    detectedKey = uiState.detectedKey,
                    detectedScale = uiState.detectedScale,
                    accentColor = uiState.activeMusician.accentColor,
                    isSoloMuted = uiState.isSoloMuted,
                    onToggleSoloMute = { viewModel.toggleSoloMute() },
                    musicianName = uiState.activeMusician.name,
                    audioSourceLabel = if (uiState.audioOutputDestination == AudioOutputDestination.LOCAL) "Local Theory Engine" else "Cloud AI (${uiState.activeLlmProvider.displayName})"
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Standard Music Notation 2-Bar Comparison View
                MusicNotationComparisonCard(
                    localNotes = uiState.localBatchedNotes,
                    cloudNotes = uiState.cloudBatchedNotes,
                    barRange = uiState.batchedBarRange,
                    chordNames = uiState.batchedChordNames,
                    currentBar = uiState.currentPlayingBar,
                    currentSub = uiState.currentPlayingSub,
                    isJamming = uiState.isJamming,
                    audioDestination = uiState.audioOutputDestination,
                    onSelectAudioDestination = { viewModel.setAudioOutputDestination(it) },
                    onRefreshCloudBatch = { viewModel.refreshCloudBatch() },
                    isExpanded = uiState.isNotationExpanded,
                    onToggleExpand = { viewModel.toggleNotationExpanded() },
                    isLocalBackupEnabled = uiState.isLocalBackupEnabled,
                    onToggleLocalBackup = { viewModel.toggleLocalBackup(it) },
                    isLlmFetching = uiState.isLlmFetching,
                    hasApiKey = uiState.geminiApiKey.isNotBlank(),
                    activeModelName = uiState.activeResolvedModel,
                    isFamousPieceMode = uiState.useFamousPiecePromptMode,
                    famousPieceName = uiState.activeMusician.famousPiece,
                    customPromptDirective = uiState.customAiPromptDirective,
                    onOpenAiPromptSettings = { showAiSettingsDialog = true },
                    onOpenComparisonMatrix = { viewModel.openComparisonMatrixDialog() },
                    timeSignature = uiState.timeSignature,
                    beatsPerBar = uiState.beatsPerBar
                )

                Spacer(modifier = Modifier.height(8.dp))

                MixerDeck(
                    uiState = uiState,
                    onSoloVolumeChange = { viewModel.setSoloVolume(it) },
                    onBackingVolumeChange = { viewModel.setBackingVolume(it) },
                    onMasterVolumeChange = { viewModel.setMasterVolume(it) },
                    onCrossfaderChange = { viewModel.setCrossfaderBalance(it) },
                    onToggleSoloMute = { viewModel.toggleSoloMute() },
                    onToggleBackingMute = { viewModel.toggleBackingMute() },
                    isExpanded = uiState.isMixerExpanded,
                    onToggleExpand = { viewModel.toggleMixerExpanded() }
                )
            }

            // 2. SECTION: "Backing Track & Input"
            CollapsibleSection(
                title = "Backing Track & Input",
                subtitle = uiState.activeTrack.title,
                badgeText = "${uiState.detectedBpm} BPM • ${uiState.timeSignature}",
                badgeColor = Color(0xFF06B6D4),
                isExpanded = isBackingExpanded,
                onToggle = { isBackingExpanded = !isBackingExpanded },
                testTag = "section_backing"
            ) {
                BackingDeck(
                    activeTrack = uiState.activeTrack,
                    allTracks = if (viewModel.allBuiltInTracks.any { it.id == uiState.activeTrack.id }) {
                        viewModel.allBuiltInTracks
                    } else {
                        listOf(uiState.activeTrack) + viewModel.allBuiltInTracks
                    },
                    isJamming = uiState.isJamming,
                    isMicRecording = uiState.isMicRecording,
                    backingVolume = uiState.backingVolume,
                    masterVolume = uiState.masterVolume,
                    onSelectTrack = { viewModel.selectTrack(it) },
                    onToggleJam = { viewModel.toggleJam() },
                    onToggleMicRecording = {
                        if (uiState.isMicRecording) {
                            viewModel.stopMicRecording()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onImportFile = { uri, name ->
                        viewModel.loadLocalAudioFile(uri, name)
                    },
                    onSetBackingVolume = { viewModel.setBackingVolume(it) },
                    onSetMasterVolume = { viewModel.setMasterVolume(it) }
                )
            }

            // 3. SECTION: "Musician Influence"
            CollapsibleSection(
                title = "Musician Influence",
                subtitle = uiState.activeMusician.eraOrGenre,
                badgeText = uiState.activeMusician.name,
                badgeColor = Color(0xFFF59E0B),
                isExpanded = isMusicianExpanded,
                onToggle = { isMusicianExpanded = !isMusicianExpanded },
                testTag = "section_musician"
            ) {
                MusicianInfluenceDeck(
                    selectedMusician = uiState.activeMusician,
                    onSelectMusician = { viewModel.selectMusician(it) }
                )
            }

            // 4. SECTION: "Lead Instrument & DSP"
            CollapsibleSection(
                title = "Lead Instrument & DSP",
                subtitle = "Cutoff ${(uiState.filterCutoff * 100).toInt()}% • Drive ${(uiState.driveAmount * 100).toInt()}%",
                badgeText = uiState.activeInstrument.displayName,
                badgeColor = Color(0xFFA78BFA),
                isExpanded = isInstrumentExpanded,
                onToggle = { isInstrumentExpanded = !isInstrumentExpanded },
                testTag = "section_instrument"
            ) {
                InstrumentDeck(
                    activeInstrument = uiState.activeInstrument,
                    driveAmount = uiState.driveAmount,
                    filterCutoff = uiState.filterCutoff,
                    soloDensity = uiState.soloDensity,
                    creativity = uiState.creativity,
                    isSoloMuted = uiState.isSoloMuted,
                    soloVolume = uiState.soloVolume,
                    onSelectInstrument = { viewModel.selectInstrument(it) },
                    onSetDrive = { viewModel.setDrive(it) },
                    onSetFilterCutoff = { viewModel.setFilterCutoff(it) },
                    onSetSoloDensity = { viewModel.setSoloDensity(it) },
                    onSetCreativity = { viewModel.setCreativity(it) },
                    onSetPitchBend = { viewModel.setPitchBend(it) },
                    onTriggerNote = { viewModel.triggerSoloNote(it) },
                    onToggleSoloMute = { viewModel.toggleSoloMute() },
                    onSetSoloVolume = { viewModel.setSoloVolume(it) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showAiSettingsDialog) {
        AiSettingsDialog(
            uiState = uiState,
            onDismiss = { showAiSettingsDialog = false },
            onSelectProvider = { viewModel.setLlmProvider(it) },
            onSaveApiKey = { provider, key -> viewModel.saveApiKey(provider, key) },
            onToggleLookahead = { viewModel.toggleLookaheadBatching(it) },
            onToggleLocalBackup = { viewModel.toggleLocalBackup(it) },
            onToggleFamousPiecePrompt = { viewModel.toggleFamousPiecePrompt(it) },
            onUpdateCustomPrompt = { viewModel.updateCustomAiPrompt(it) },
            onGetPromptPreview = { viewModel.getCompiledPromptPreview() },
            onTestConnection = { provider, key, callback -> viewModel.testLlmConnection(provider, key, callback) }
        )
    }

    if (uiState.showComparisonMatrixDialog) {
        ComparisonMatrixDialog(
            initialMusicianId = uiState.activeMusician.id,
            trackTitle = uiState.activeTrack.title,
            key = uiState.detectedKey,
            bpm = uiState.detectedBpm,
            timeSignature = uiState.timeSignature,
            beatsPerBar = uiState.beatsPerBar,
            onDismiss = { viewModel.closeComparisonMatrixDialog() },
            onLaunchSession = { viewModel.launchComparisonSession(it) }
        )
    }

    if (uiState.showComparisonGalleryDialog && uiState.activeComparisonSession != null) {
        ComparisonGalleryDialog(
            session = uiState.activeComparisonSession!!,
            results = uiState.comparisonResults,
            isGenerating = uiState.isComparisonGenerating,
            onDismiss = { viewModel.closeComparisonGalleryDialog() },
            onReRollVariant = { viewModel.reRollComparisonVariant(it) },
            onAuditionVariant = { notes, bpm, beats -> viewModel.auditionVariant(notes, bpm, beats) },
            onStopAudition = { viewModel.stopAudition() },
            isAuditioning = uiState.isAuditioning,
            auditionVariantId = uiState.auditionVariantId
        )
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFF38BDF8),
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String,
    content: @Composable () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevronRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF131722)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isExpanded) badgeColor.copy(alpha = 0.5f) else Color(0xFF1E293B)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (badgeText != null) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = badgeColor,
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(badgeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse $title" else "Expand $title",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.rotate(rotationState).size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 0.dp)
                ) {
                    HorizontalDivider(
                        color = Color(0xFF1E293B),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    content()
                }
            }
        }
    }
}
