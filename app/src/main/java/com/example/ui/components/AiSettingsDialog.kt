package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.LlmProvider
import com.example.ui.StudioUiState

@Composable
fun AiSettingsDialog(
    uiState: StudioUiState,
    onDismiss: () -> Unit,
    onSelectProvider: (LlmProvider) -> Unit,
    onSaveApiKey: (LlmProvider, String) -> Unit,
    onToggleLookahead: (Boolean) -> Unit,
    onToggleLocalBackup: (Boolean) -> Unit,
    onToggleFamousPiecePrompt: (Boolean) -> Unit = {},
    onUpdateCustomPrompt: (String) -> Unit = {},
    onGetPromptPreview: () -> String = { "" },
    onTestConnection: (LlmProvider, String, (Boolean, String) -> Unit) -> Unit
) {
    var selectedProvider by remember {
        mutableStateOf(if (uiState.activeLlmProvider == LlmProvider.LOCAL_THEORY) LlmProvider.GEMINI else uiState.activeLlmProvider)
    }

    var geminiKeyInput by remember(uiState.geminiApiKey) { mutableStateOf(uiState.geminiApiKey) }
    var openAiKeyInput by remember(uiState.openAiApiKey) { mutableStateOf(uiState.openAiApiKey) }
    var claudeKeyInput by remember(uiState.claudeApiKey) { mutableStateOf(uiState.claudeApiKey) }

    var promptDirectiveInput by remember(uiState.customAiPromptDirective) { mutableStateOf(uiState.customAiPromptDirective) }
    var showPromptPreview by remember { mutableStateOf(false) }
    var promptSavedStatus by remember { mutableStateOf<String?>(null) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    var showClaudeKey by remember { mutableStateOf(false) }

    var testStatusText by remember { mutableStateOf<String?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var isTestSuccess by remember { mutableStateOf<Boolean?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF131722),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("dialog_ai_settings")
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ==========================================
                // 1. PINNED HEADER (Always visible)
                // ==========================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Lookahead",
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CLOUD AI & LOOKAHEAD",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Engine modes, providers & prompt directives",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("button_close_ai_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // ==========================================
                // 2. SCROLLABLE CONTENT BODY
                // ==========================================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // ------------------------------------------
                    // CONSOLIDATED ENGINE & PROMPT MODES CARD
                    // ------------------------------------------
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2433),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "IMPROV ENGINE & BATCHING MODES",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // ROW 1: Cloud Lookahead Batching (Always On by Default)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Lookahead Batching",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "0ms Latency",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = "Pre-fetches 2 bars in background so notes play on downbeat.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                                        .border(1.dp, Color(0xFF8B5CF6), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "ALWAYS ON",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA78BFA),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = Color(0xFF283347),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // ROW 2: Local Engine Backup
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Local Theory Backup",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (uiState.isLocalBackupEnabled) "BACKUP ON" else "PURE CLOUD",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.isLocalBackupEnabled) Color(0xFF10B981) else Color(0xFFF43F5E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (uiState.isLocalBackupEnabled) Color(0xFF10B981).copy(alpha = 0.15f)
                                                    else Color(0xFFF43F5E).copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = if (uiState.isLocalBackupEnabled)
                                            "Falls back to local algorithm if cloud buffer underruns."
                                        else
                                            "Disabled. Pure cloud synthesis (rests if buffer is empty).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.isLocalBackupEnabled,
                                    onCheckedChange = { onToggleLocalBackup(it) },
                                    modifier = Modifier.testTag("switch_local_backup"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF10B981),
                                        uncheckedThumbColor = Color(0xFFCBD5E1),
                                        uncheckedTrackColor = Color(0xFF334155)
                                    )
                                )
                            }

                            HorizontalDivider(
                                color = Color(0xFF283347),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            // ROW 3: Famous Piece Prompt Mode
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Famous Piece Prompt",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (uiState.useFamousPiecePromptMode) "SONG REPERTOIRE" else "ANALYTICAL PARAMS",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (uiState.useFamousPiecePromptMode) Color(0xFFA855F7) else Color(0xFF94A3B8),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (uiState.useFamousPiecePromptMode) Color(0xFFA855F7).copy(alpha = 0.2f)
                                                    else Color(0xFF334155).copy(alpha = 0.3f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                    Text(
                                        text = if (uiState.useFamousPiecePromptMode)
                                            "Queries model with iconic piece: \"${uiState.activeMusician.famousPiece}\"."
                                        else
                                            "Standard prompt with math parameters (Density ${(uiState.activeMusician.noteDensity * 100).toInt()}%).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = uiState.useFamousPiecePromptMode,
                                    onCheckedChange = { onToggleFamousPiecePrompt(it) },
                                    modifier = Modifier.testTag("switch_famous_piece_prompt"),
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFA855F7),
                                        uncheckedThumbColor = Color(0xFFCBD5E1),
                                        uncheckedTrackColor = Color(0xFF334155)
                                    )
                                )
                            }
                        }
                    }

                    // ------------------------------------------
                    // ACTIVE MODEL PROVIDER (COMPACT 2x2 TILES)
                    // ------------------------------------------
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "ACTIVE MODEL PROVIDER",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        val providers = remember { LlmProvider.values().filter { it != LlmProvider.LOCAL_THEORY } }
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in 0 until providers.size step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    for (j in 0..1) {
                                        if (i + j < providers.size) {
                                            val provider = providers[i + j]
                                            val isSelected = (selectedProvider == provider)
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isSelected) Color(0xFF8B5CF6).copy(alpha = 0.22f) else Color(0xFF1A2234),
                                                border = BorderStroke(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) Color(0xFFA78BFA) else Color(0xFF2E3B52)
                                                ),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        selectedProvider = provider
                                                        onSelectProvider(provider)
                                                        testStatusText = null
                                                    }
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = provider.displayName,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1),
                                                            fontSize = 13.sp,
                                                            maxLines = 1
                                                        )
                                                        if (isSelected) {
                                                            Text(
                                                                text = "ACTIVE",
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFFA78BFA),
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(3.dp))
                                                                    .background(Color(0xFF8B5CF6).copy(alpha = 0.3f))
                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = if (provider == LlmProvider.LOCAL_THEORY) "On-Device Engine" else provider.defaultModel,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (isSelected) Color(0xFFC4B5FD) else Color(0xFF64748B),
                                                        fontSize = 10.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ------------------------------------------
                    // API KEY CONFIGURATION FOR SELECTED CLOUD PROVIDER
                    // ------------------------------------------
                    AnimatedVisibility(visible = selectedProvider != LlmProvider.LOCAL_THEORY) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val currentKeyInput = when (selectedProvider) {
                                LlmProvider.GEMINI -> geminiKeyInput
                                LlmProvider.OPENAI -> openAiKeyInput
                                LlmProvider.CLAUDE -> claudeKeyInput
                                LlmProvider.LOCAL_THEORY -> ""
                            }
                            val showKey = when (selectedProvider) {
                                LlmProvider.GEMINI -> showGeminiKey
                                LlmProvider.OPENAI -> showOpenAiKey
                                LlmProvider.CLAUDE -> showClaudeKey
                                LlmProvider.LOCAL_THEORY -> false
                            }

                            Text(
                                text = "${selectedProvider.displayName.uppercase()} API KEY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                letterSpacing = 1.sp
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = currentKeyInput,
                                onValueChange = { newVal ->
                                    when (selectedProvider) {
                                        LlmProvider.GEMINI -> geminiKeyInput = newVal
                                        LlmProvider.OPENAI -> openAiKeyInput = newVal
                                        LlmProvider.CLAUDE -> claudeKeyInput = newVal
                                        LlmProvider.LOCAL_THEORY -> {}
                                    }
                                },
                                placeholder = { Text("Paste your API key here...", color = Color(0xFF64748B), fontSize = 12.sp) },
                                singleLine = true,
                                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                trailingIcon = {
                                    IconButton(onClick = {
                                        when (selectedProvider) {
                                            LlmProvider.GEMINI -> showGeminiKey = !showGeminiKey
                                            LlmProvider.OPENAI -> showOpenAiKey = !showOpenAiKey
                                            LlmProvider.CLAUDE -> showClaudeKey = !showClaudeKey
                                            LlmProvider.LOCAL_THEORY -> {}
                                        }
                                    }) {
                                        Icon(
                                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle visibility",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFA78BFA),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF1E2433),
                                    unfocusedContainerColor = Color(0xFF1E2433)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_api_key")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Save Key Button
                                Button(
                                    onClick = {
                                        onSaveApiKey(selectedProvider, currentKeyInput)
                                        testStatusText = "Key saved for ${selectedProvider.displayName}."
                                        isTestSuccess = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("button_save_api_key")
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Key", fontSize = 12.sp)
                                }

                                // Test Connection Button
                                OutlinedButton(
                                    onClick = {
                                        onSaveApiKey(selectedProvider, currentKeyInput)
                                        isTesting = true
                                        testStatusText = "Testing connection to ${selectedProvider.displayName}..."
                                        isTestSuccess = null
                                        onTestConnection(selectedProvider, currentKeyInput) { success, msg ->
                                            isTesting = false
                                            isTestSuccess = success
                                            testStatusText = msg
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    enabled = currentKeyInput.isNotBlank() && !isTesting,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .testTag("button_test_api_key")
                                ) {
                                    if (isTesting) {
                                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Pinging...", fontSize = 12.sp)
                                    } else {
                                        Icon(imageVector = Icons.Default.Bolt, contentDescription = "Test", modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Test Ping", fontSize = 12.sp)
                                    }
                                }
                            }

                            // Test Status Banner
                            testStatusText?.let { status ->
                                Spacer(modifier = Modifier.height(6.dp))
                                val isOk = isTestSuccess == true
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isOk) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isOk) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (isOk) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = status,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isOk) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Get key at ${selectedProvider.docsUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF64748B),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // ------------------------------------------
                    // CUSTOM AI PROMPT DIRECTIVES EDITOR
                    // ------------------------------------------
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E2433),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.EditNote,
                                        contentDescription = null,
                                        tint = Color(0xFFFBBF24),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "AI IMPROV PROMPT DIRECTIVES",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "Custom phrasing and style rules sent to cloud model",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = promptDirectiveInput,
                                onValueChange = {
                                    promptDirectiveInput = it
                                    promptSavedStatus = null
                                },
                                placeholder = {
                                    Text(
                                        text = "e.g. Focus on blues bends, syncopated 16th rhythms...",
                                        color = Color(0xFF64748B),
                                        fontSize = 12.sp
                                    )
                                },
                                minLines = 2,
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFFBBF24),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF131722),
                                    unfocusedContainerColor = Color(0xFF131722)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_custom_prompt_directive")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Style Preset Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        promptDirectiveInput = "Play soulful blues phrases with heavy half-step bends, blue note passing tones, and emotional call-and-response."
                                        promptSavedStatus = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF263248),
                                    modifier = Modifier.weight(1f).testTag("preset_blues")
                                ) {
                                    Text(
                                        text = "🎸 Blues",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24),
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    onClick = {
                                        promptDirectiveInput = "Use fast 16th-note bebop enclosures, arpeggiated upper chord extensions, and rapid swing syncopation."
                                        promptSavedStatus = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF263248),
                                    modifier = Modifier.weight(1f).testTag("preset_bebop")
                                ) {
                                    Text(
                                        text = "🎷 Bebop",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    onClick = {
                                        promptDirectiveInput = "Play spacious, atmospheric melodic lines with long sustained notes, gentle dynamics, and ample breathing room."
                                        promptSavedStatus = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF263248),
                                    modifier = Modifier.weight(1f).testTag("preset_ambient")
                                ) {
                                    Text(
                                        text = "🌌 Ambient",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA78BFA),
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }

                                Surface(
                                    onClick = {
                                        promptDirectiveInput = ""
                                        promptSavedStatus = null
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.weight(0.7f).testTag("preset_reset")
                                ) {
                                    Text(
                                        text = "Reset",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF94A3B8),
                                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Action buttons: Save & Toggle Preview
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onUpdateCustomPrompt(promptDirectiveInput)
                                        promptSavedStatus = "Directive saved! Re-querying AI batch..."
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("button_save_custom_prompt")
                                ) {
                                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(13.dp), tint = Color.Black)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save Directive", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = { showPromptPreview = !showPromptPreview },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .testTag("button_toggle_prompt_preview")
                                ) {
                                    Icon(
                                        imageVector = if (showPromptPreview) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(13.dp),
                                        tint = Color(0xFF94A3B8)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (showPromptPreview) "Hide Prompt" else "Preview Full",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Success Confirmation
                            promptSavedStatus?.let { msg ->
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    Text(
                                        text = "✓ $msg",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Full Prompt Monospace Preview (Scrollable with max height)
                            AnimatedVisibility(visible = showPromptPreview) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "COMPILED PROMPT SENT TO MODEL:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFBBF24),
                                        fontSize = 9.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF090D16),
                                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 140.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .verticalScroll(rememberScrollState())
                                                .padding(8.dp)
                                        ) {
                                            Text(
                                                text = onGetPromptPreview(),
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color(0xFFE2E8F0),
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ==========================================
                // 3. PINNED FOOTER (Always visible action)
                // ==========================================
                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("button_done_ai_settings"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5CF6),
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Apply Selection",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
