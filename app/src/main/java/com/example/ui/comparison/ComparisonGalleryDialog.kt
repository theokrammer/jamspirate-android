package com.example.ui.comparison

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.LookaheadNote
import com.example.comparison.ComparisonSession
import com.example.comparison.ComparisonVariantResult
import com.example.export.SheetMusicPdfExporter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class GalleryViewMode(val title: String) {
    STACKED_SCORE("Score View (Stacked)"),
    LEAD_SHEET("Lead Sheet (Tabs)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComparisonGalleryDialog(
    session: ComparisonSession,
    results: List<ComparisonVariantResult>,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onReRollVariant: (String) -> Unit,
    onAuditionVariant: (List<LookaheadNote>, Int, Int) -> Unit, // notes, bpm, beatsPerBar
    onStopAudition: () -> Unit,
    isAuditioning: Boolean,
    auditionVariantId: String?
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var viewMode by remember { mutableStateOf(GalleryViewMode.STACKED_SCORE) }
    var selectedTabVariantId by remember(results) {
        mutableStateOf(results.firstOrNull()?.variant?.id ?: "")
    }

    Dialog(
        onDismissRequest = {
            onStopAudition()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f)
                .fillMaxHeight(0.96f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .testTag("dialog_comparison_gallery"),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // 1. Top Header & Action Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "IMPROV COMPARISON GALLERY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }
                        Text(
                            text = "${session.trackTitle} • Key ${session.key} • ${session.timeSignature} • ${session.barCount} Measures",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Print Button
                        IconButton(
                            onClick = {
                                if (activity != null && results.isNotEmpty()) {
                                    val pdf = SheetMusicPdfExporter.generateComparisonPdf(context, session, results)
                                    SheetMusicPdfExporter.printPdf(activity, pdf)
                                }
                            },
                            modifier = Modifier.size(34.dp).testTag("btn_print_score")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print Score", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                        }

                        // Share / PDF Button
                        Button(
                            onClick = {
                                if (results.isNotEmpty()) {
                                    val pdf = SheetMusicPdfExporter.generateComparisonPdf(context, session, results)
                                    SheetMusicPdfExporter.sharePdf(context, pdf)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(34.dp).testTag("btn_export_pdf")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Close Button
                        IconButton(
                            onClick = {
                                onStopAudition()
                                onDismiss()
                            },
                            modifier = Modifier.size(32.dp).testTag("btn_close_gallery")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. View Mode Switcher & Audition Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mode Switcher Tabs
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF131722))
                            .padding(2.dp)
                    ) {
                        GalleryViewMode.values().forEach { mode ->
                            val active = viewMode == mode
                            Surface(
                                onClick = { viewMode = mode },
                                shape = RoundedCornerShape(6.dp),
                                color = if (active) Color(0xFF0284C7) else Color.Transparent
                            ) {
                                Text(
                                    text = mode.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                    color = if (active) Color.White else Color(0xFF94A3B8),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Audition Status / Stop Button
                    if (isAuditioning) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF059669).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFF10B981), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Auditioning Active",
                                fontSize = 10.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = onStopAudition,
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Body Content
                val textMeasurer = rememberTextMeasurer()

                when (viewMode) {
                    GalleryViewMode.STACKED_SCORE -> {
                        StackedScoreGalleryView(
                            session = session,
                            results = results,
                            textMeasurer = textMeasurer,
                            isAuditioning = isAuditioning,
                            auditionVariantId = auditionVariantId,
                            onAuditionVariant = onAuditionVariant,
                            onStopAudition = onStopAudition,
                            onReRollVariant = onReRollVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    GalleryViewMode.LEAD_SHEET -> {
                        TabbedLeadSheetView(
                            session = session,
                            results = results,
                            selectedVariantId = selectedTabVariantId,
                            onSelectVariant = { selectedTabVariantId = it },
                            textMeasurer = textMeasurer,
                            isAuditioning = isAuditioning,
                            auditionVariantId = auditionVariantId,
                            onAuditionVariant = onAuditionVariant,
                            onStopAudition = onStopAudition,
                            onReRollVariant = onReRollVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StackedScoreGalleryView(
    session: ComparisonSession,
    results: List<ComparisonVariantResult>,
    textMeasurer: TextMeasurer,
    isAuditioning: Boolean,
    auditionVariantId: String?,
    onAuditionVariant: (List<LookaheadNote>, Int, Int) -> Unit,
    onStopAudition: () -> Unit,
    onReRollVariant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScroll = rememberScrollState()
    val verticalScroll = rememberScrollState()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D121F)),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8).copy(alpha = 0.3f), Color(0xFF1E293B))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Scrollable horizontally across all measures
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
            ) {
                val measureWidthDp = 180.dp
                val totalWidthDp = 70.dp + (measureWidthDp * session.barCount)

                Column(
                    modifier = Modifier
                        .width(totalWidthDp)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Top Chords & Measure Bar Markers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 70.dp)
                    ) {
                        for (barIdx in 0 until session.barCount) {
                            val chord = session.chords.find { it.first == barIdx }?.second?.chordName
                                ?: if (barIdx % 2 == 0) "Am7" else "Dm7"
                            Box(
                                modifier = Modifier
                                    .width(measureWidthDp)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF1E293B))
                                    .padding(vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Bar ${barIdx + 1}: $chord",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // Render Staves for each variant
                    results.forEachIndexed { vIdx, res ->
                        val variant = res.variant
                        val isThisAuditioning = isAuditioning && (auditionVariantId == variant.id)

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070B14))
                                .border(1.dp, variant.color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(vertical = 6.dp)
                        ) {
                            // Variant Sub-Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(variant.color)
                                    )
                                    Text(
                                        text = "[${'A' + vIdx}] ${variant.label} (${res.musician.name})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = variant.color
                                    )
                                    Text(
                                        text = "• ${res.notes.size} notes • ${res.latencyMs}ms",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            if (isThisAuditioning) {
                                                onStopAudition()
                                            } else {
                                                onAuditionVariant(res.notes, session.bpm, session.beatsPerBar)
                                            }
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isThisAuditioning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = "Audition",
                                            tint = if (isThisAuditioning) Color(0xFFEF4444) else Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { onReRollVariant(variant.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Re-roll",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Staff Canvas
                            MultiBarStaffCanvas(
                                session = session,
                                notes = res.notes,
                                noteColor = variant.color,
                                textMeasurer = textMeasurer,
                                measureWidthDp = measureWidthDp,
                                modifier = Modifier
                                    .width(totalWidthDp)
                                    .height(96.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabbedLeadSheetView(
    session: ComparisonSession,
    results: List<ComparisonVariantResult>,
    selectedVariantId: String,
    onSelectVariant: (String) -> Unit,
    textMeasurer: TextMeasurer,
    isAuditioning: Boolean,
    auditionVariantId: String?,
    onAuditionVariant: (List<LookaheadNote>, Int, Int) -> Unit,
    onStopAudition: () -> Unit,
    onReRollVariant: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeResult = results.find { it.variant.id == selectedVariantId } ?: results.firstOrNull()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tab Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            results.forEachIndexed { idx, res ->
                val isSelected = res.variant.id == (activeResult?.variant?.id ?: "")
                Surface(
                    onClick = { onSelectVariant(res.variant.id) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) res.variant.color.copy(alpha = 0.25f) else Color(0xFF131722),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                if (isSelected) res.variant.color else Color(0xFF1E293B),
                                if (isSelected) res.variant.color.copy(alpha = 0.5f) else Color(0xFF1E293B)
                            )
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(res.variant.color)
                        )
                        Text(
                            text = "[${'A' + idx}] ${res.variant.label}",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) res.variant.color else Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        if (activeResult != null) {
            val variant = activeResult.variant
            val isThisAuditioning = isAuditioning && (auditionVariantId == variant.id)

            // Variant Details & Metrics Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${variant.label} — Master: ${activeResult.musician.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = variant.color
                            )
                            Text(
                                text = if (variant.useFamousPieceMode) {
                                    "Theme: ${variant.famousPieceName.ifBlank { activeResult.musician.famousPiece }}"
                                } else {
                                    variant.customDirective.ifBlank { activeResult.musician.signatureStyle }
                                },
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                maxLines = 2
                            )
                        }

                        Button(
                            onClick = {
                                if (isThisAuditioning) {
                                    onStopAudition()
                                } else {
                                    onAuditionVariant(activeResult.notes, session.bpm, session.beatsPerBar)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isThisAuditioning) Color(0xFFEF4444) else Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(if (isThisAuditioning) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isThisAuditioning) "Stop" else "Audition", fontSize = 11.sp)
                        }
                    }

                    // Stats strip
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF070B14))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Notes: ${activeResult.notes.size}", fontSize = 10.sp, color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace)
                        Text("Density: ${String.format("%.1f", activeResult.averageDensityPerBar)}/bar", fontSize = 10.sp, color = Color(0xFFF59E0B), fontFamily = FontFamily.Monospace)
                        Text("Range: ${activeResult.pitchRangeSemitones} semitones", fontSize = 10.sp, color = Color(0xFF10B981), fontFamily = FontFamily.Monospace)
                        Text("Latency: ${activeResult.latencyMs}ms", fontSize = 10.sp, color = Color(0xFFA78BFA), fontFamily = FontFamily.Monospace)
                    }
                }
            }

            // Lead sheet staff view (scrollable)
            val measureWidthDp = 180.dp
            val totalWidthDp = 70.dp + (measureWidthDp * session.barCount)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            ) {
                MultiBarStaffCanvas(
                    session = session,
                    notes = activeResult.notes,
                    noteColor = variant.color,
                    textMeasurer = textMeasurer,
                    measureWidthDp = measureWidthDp,
                    modifier = Modifier
                        .width(totalWidthDp)
                        .height(130.dp)
                )
            }
        }
    }
}

/**
 * Custom Canvas drawing multi-bar standard 5-line staff with horizontal measure lines.
 */
@Composable
private fun MultiBarStaffCanvas(
    session: ComparisonSession,
    notes: List<LookaheadNote>,
    noteColor: Color,
    textMeasurer: TextMeasurer,
    measureWidthDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF060911))
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        val staffStartX = 56.dp.toPx()
        val measureWidthPx = measureWidthDp.toPx()
        val lineSpacing = 7.dp.toPx()
        val staffCenterY = canvasHeight / 2f

        // 5 Staff Lines
        for (i in -2..2) {
            val y = staffCenterY + (i * lineSpacing)
            drawLine(
                color = Color(0xFF334155),
                start = Offset(staffStartX - 6.dp.toPx(), y),
                end = Offset(canvasWidth - 8.dp.toPx(), y),
                strokeWidth = 1.1.dp.toPx()
            )
        }

        // Measure Bar Lines
        for (m in 0..session.barCount) {
            val bx = staffStartX + (m * measureWidthPx)
            val isFinal = m == session.barCount
            if (isFinal) {
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(bx - 3.dp.toPx(), staffCenterY - 2 * lineSpacing),
                    end = Offset(bx - 3.dp.toPx(), staffCenterY + 2 * lineSpacing),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = Color(0xFF94A3B8),
                    start = Offset(bx, staffCenterY - 2 * lineSpacing),
                    end = Offset(bx, staffCenterY + 2 * lineSpacing),
                    strokeWidth = 2.5.dp.toPx()
                )
            } else {
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(bx, staffCenterY - 2 * lineSpacing),
                    end = Offset(bx, staffCenterY + 2 * lineSpacing),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }

        // Treble Clef 𝄞 & Meter
        drawText(
            textMeasurer = textMeasurer,
            text = "𝄞",
            topLeft = Offset(8.dp.toPx(), staffCenterY - 24.dp.toPx()),
            style = TextStyle(
                fontSize = 30.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold
            )
        )
        val timeSigParts = session.timeSignature.split("/")
        val top = timeSigParts.getOrNull(0) ?: "${session.beatsPerBar}"
        val bottom = timeSigParts.getOrNull(1) ?: "4"
        drawText(
            textMeasurer = textMeasurer,
            text = "$top\n$bottom",
            topLeft = Offset(36.dp.toPx(), staffCenterY - 13.dp.toPx()),
            style = TextStyle(
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace
            )
        )

        val subdivisionsPerBar = (session.beatsPerBar * 4).coerceAtLeast(4)

        // Draw Notes
        for (note in notes) {
            if (note.barIndex !in 0 until session.barCount) continue

            val noteX = staffStartX + (note.barIndex * measureWidthPx) +
                    ((note.subInBar.coerceIn(0, subdivisionsPerBar - 1) + 0.5f) / subdivisionsPerBar.toFloat()) * (measureWidthPx - 10.dp.toPx()) + 4.dp.toPx()

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
            val stepFromB4 = totalDiatonicStep - 34 // B4 is center line
            val noteY = staffCenterY - (stepFromB4 * (lineSpacing / 2f))

            // Ledger lines
            if (stepFromB4 <= -2 && stepFromB4 % 2 == 0) {
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(noteX - 6.dp.toPx(), noteY),
                    end = Offset(noteX + 6.dp.toPx(), noteY),
                    strokeWidth = 1.1.dp.toPx()
                )
            } else if (stepFromB4 >= 6 && stepFromB4 % 2 == 0) {
                drawLine(
                    color = Color(0xFF64748B),
                    start = Offset(noteX - 6.dp.toPx(), noteY),
                    end = Offset(noteX + 6.dp.toPx(), noteY),
                    strokeWidth = 1.1.dp.toPx()
                )
            }

            // Sharp sign
            if (isSharp) {
                drawText(
                    textMeasurer = textMeasurer,
                    text = "♯",
                    topLeft = Offset(noteX - 9.dp.toPx(), noteY - 7.dp.toPx()),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = noteColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Notehead
            drawOval(
                color = noteColor,
                topLeft = Offset(noteX - 4.5.dp.toPx(), noteY - 3.2.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(9.dp.toPx(), 6.5.dp.toPx())
            )

            // Stem
            val stemLen = 18.dp.toPx()
            if (stepFromB4 < 0) {
                drawLine(
                    color = noteColor,
                    start = Offset(noteX + 4.5.dp.toPx(), noteY),
                    end = Offset(noteX + 4.5.dp.toPx(), noteY - stemLen),
                    strokeWidth = 1.3.dp.toPx()
                )
            } else {
                drawLine(
                    color = noteColor,
                    start = Offset(noteX - 4.5.dp.toPx(), noteY),
                    end = Offset(noteX - 4.5.dp.toPx(), noteY + stemLen),
                    strokeWidth = 1.3.dp.toPx()
                )
            }
        }
    }
}
