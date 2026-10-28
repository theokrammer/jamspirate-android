package com.example.ui.comparison

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.comparison.ComparisonPreset
import com.example.comparison.ComparisonPresets
import com.example.comparison.ComparisonSession
import com.example.comparison.ComparisonVariant
import com.example.model.MusicianProfile
import com.example.model.MusicianRegistry

private val SLOT_COLORS = listOf(
    0xFF38BDF8, // Sky Blue
    0xFFF59E0B, // Amber
    0xFF10B981, // Emerald
    0xFFA855F7  // Purple
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonMatrixDialog(
    initialMusicianId: String,
    trackTitle: String,
    key: String,
    bpm: Int,
    timeSignature: String,
    beatsPerBar: Int,
    onDismiss: () -> Unit,
    onLaunchSession: (ComparisonSession) -> Unit
) {
    val presets = remember { ComparisonPresets.createDefaultPresets(initialMusicianId) }
    var selectedBarCount by remember { mutableStateOf(4) } // 4 or 8 bars

    var variants by remember {
        mutableStateOf(presets.first().variants.toMutableList())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .testTag("dialog_comparison_matrix"),
            color = Color(0xFF0B0F19)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF0284C7), Color(0xFF8B5CF6))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "IMPROV COMPARISON MATRIX",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "$trackTitle • Key: $key • $timeSignature • Fast background batch",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).testTag("btn_close_matrix")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 2. Quick Presets Row
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "QUICK COMPARISON PRESETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presets) { preset ->
                                val isSelected = variants.size == preset.variants.size &&
                                        variants.firstOrNull()?.label == preset.variants.firstOrNull()?.label
                                Surface(
                                    onClick = { variants = preset.variants.toMutableList() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0xFF0369A1) else Color(0xFF1E293B),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = Brush.horizontalGradient(
                                            listOf(
                                                if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                                if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                                            )
                                        )
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                        Text(
                                            text = preset.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                        )
                                        Text(
                                            text = "${preset.variants.size} Variants",
                                            fontSize = 9.sp,
                                            color = if (isSelected) Color(0xFFBAE6FD) else Color(0xFF64748B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Excerpt Length Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF131722))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "COMPARISON EXCERPT LENGTH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC),
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Measures to generate and score together",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(4, 8).forEach { count ->
                                val selected = selectedBarCount == count
                                Surface(
                                    onClick = { selectedBarCount = count },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (selected) Color(0xFF0284C7) else Color(0xFF1E293B),
                                    modifier = Modifier.testTag("btn_bar_count_$count")
                                ) {
                                    Text(
                                        text = "$count Bars",
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) Color.White else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 4. Variant Slots List
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CONFIGURED VARIANTS (${variants.size} of 4)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace
                            )

                            if (variants.size < 4) {
                                TextButton(
                                    onClick = {
                                        val idx = variants.size
                                        val color = SLOT_COLORS[idx % SLOT_COLORS.size]
                                        variants = (variants + ComparisonVariant(
                                            id = "slot_${System.currentTimeMillis()}",
                                            label = "Variant ${'A' + idx}",
                                            musicianId = initialMusicianId,
                                            customDirective = "Expressive dynamic solo line",
                                            accentColorArgb = color
                                        )).toMutableList()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Slot", fontSize = 11.sp)
                                }
                            }
                        }

                        variants.forEachIndexed { index, variant ->
                            VariantSlotCard(
                                slotIndex = index,
                                variant = variant,
                                canDelete = variants.size > 2,
                                onUpdate = { updated ->
                                    val list = variants.toMutableList()
                                    list[index] = updated
                                    variants = list
                                },
                                onDelete = {
                                    if (variants.size > 2) {
                                        val list = variants.toMutableList()
                                        list.removeAt(index)
                                        variants = list
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Cancel", color = Color(0xFF94A3B8))
                    }

                    Button(
                        onClick = {
                            val session = ComparisonSession(
                                trackId = "current",
                                trackTitle = trackTitle,
                                bpm = bpm,
                                key = key,
                                timeSignature = timeSignature,
                                beatsPerBar = beatsPerBar,
                                barCount = selectedBarCount,
                                variants = variants
                            )
                            onLaunchSession(session)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(2f)
                            .height(46.dp)
                            .testTag("btn_run_matrix_comparison")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Batch Generate & Compare",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantSlotCard(
    slotIndex: Int,
    variant: ComparisonVariant,
    canDelete: Boolean,
    onUpdate: (ComparisonVariant) -> Unit,
    onDelete: () -> Unit
) {
    val letter = ('A' + slotIndex)
    val musician = MusicianRegistry.getById(variant.musicianId)
    var isExpanded by remember { mutableStateOf(false) }
    var showMusicianMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131722)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(variant.color.copy(alpha = 0.6f), Color(0xFF1E293B))
            )
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(variant.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$letter",
                            color = Color.Black,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = variant.label.ifBlank { "Variant $letter" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Master: ${musician.name}",
                            fontSize = 10.sp,
                            color = variant.color,
                            maxLines = 1
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (isExpanded) "Hide" else "Customize", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }

                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = Color(0xFF1E293B))

                    // Musician Selector Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Musician Influence:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Box {
                            Surface(
                                onClick = { showMusicianMenu = true },
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF1E293B)
                            ) {
                                Text(
                                    text = musician.name,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showMusicianMenu,
                                onDismissRequest = { showMusicianMenu = false }
                            ) {
                                MusicianRegistry.ALL_MUSICIANS.forEach { profile ->
                                    DropdownMenuItem(
                                        text = { Text(profile.name, fontSize = 12.sp) },
                                        onClick = {
                                            onUpdate(variant.copy(musicianId = profile.id))
                                            showMusicianMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Mode: Famous Piece motif vs Custom Prompt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Use Famous Piece Motif:", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Switch(
                            checked = variant.useFamousPieceMode,
                            onCheckedChange = { isFamous ->
                                onUpdate(
                                    variant.copy(
                                        useFamousPieceMode = isFamous,
                                        famousPieceName = if (isFamous) musician.famousPiece else ""
                                    )
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    if (variant.useFamousPieceMode) {
                        OutlinedTextField(
                            value = variant.famousPieceName.ifBlank { musician.famousPiece },
                            onValueChange = { onUpdate(variant.copy(famousPieceName = it)) },
                            label = { Text("Reference Piece Theme", fontSize = 10.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = variant.customDirective,
                            onValueChange = { onUpdate(variant.copy(customDirective = it)) },
                            label = { Text("Prompt Directive / Stylistic Instruction", fontSize = 10.sp) },
                            textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3
                        )
                    }
                }
            }
        }
    }
}
