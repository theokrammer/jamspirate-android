package com.example.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.comparison.ComparisonSession
import com.example.comparison.ComparisonVariantResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SheetMusicPdfExporter {

    private const val PAGE_WIDTH = 595 // A4 standard width (points)
    private const val PAGE_HEIGHT = 842 // A4 standard height (points)
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 36f
    private const val MARGIN_TOP = 40f
    private const val MARGIN_BOTTOM = 40f

    /**
     * Generates a multi-stave printable vector PDF score document from a comparison session.
     */
    fun generateComparisonPdf(
        context: Context,
        session: ComparisonSession,
        results: List<ComparisonVariantResult>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawScorePage(canvas, session, results)

        document.finishPage(page)

        val outputFile = File(context.cacheDir, "jamspirate_comparison_score.pdf")
        if (outputFile.exists()) {
            outputFile.delete()
        }
        FileOutputStream(outputFile).use { out ->
            document.writeTo(out)
        }
        document.close()
        return outputFile
    }

    private fun drawScorePage(
        canvas: Canvas,
        session: ComparisonSession,
        results: List<ComparisonVariantResult>
    ) {
        // Background
        canvas.drawColor(android.graphics.Color.WHITE)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#334155")
            strokeWidth = 0.8f
            style = Paint.Style.STROKE
        }

        val staffLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            strokeWidth = 0.75f
            style = Paint.Style.STROKE
        }

        val chordPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0284C7")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        // 1. Header
        var currentY = MARGIN_TOP
        textPaint.textSize = 17f
        canvas.drawText("JAMSPIRATE STUDIO — IMPROVISATION COMPARISON", MARGIN_LEFT, currentY, textPaint)

        currentY += 16f
        val dateStr = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()).format(Date())
        val metaStr = "Track: ${session.trackTitle}  |  Key: ${session.key}  |  BPM: ${session.bpm}  |  Meter: ${session.timeSignature}  |  $dateStr"
        canvas.drawText(metaStr, MARGIN_LEFT, currentY, subPaint)

        currentY += 8f
        canvas.drawLine(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY, linePaint)
        currentY += 14f

        // 2. Legend of Variants
        val legendBoxHeight = (results.size * 14f) + 12f
        val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        }
        val legendBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#E2E8F0")
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val rect = RectF(MARGIN_LEFT, currentY, PAGE_WIDTH - MARGIN_RIGHT, currentY + legendBoxHeight)
        canvas.drawRoundRect(rect, 4f, 4f, legendPaint)
        canvas.drawRoundRect(rect, 4f, 4f, legendBorder)

        var legendY = currentY + 12f
        for ((idx, res) in results.withIndex()) {
            val letter = ('A' + idx)
            val variant = res.variant

            // Color circle
            val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor(
                    when (idx % 4) {
                        0 -> "#0284C7"
                        1 -> "#D97706"
                        2 -> "#059669"
                        else -> "#7C3AED"
                    }
                )
                style = Paint.Style.FILL
            }
            canvas.drawCircle(MARGIN_LEFT + 10f, legendY - 3f, 3.5f, dotPaint)

            val itemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#1E293B")
                textSize = 9.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            val titleText = "[$letter] ${variant.label} (${res.musician.name})"
            canvas.drawText(titleText, MARGIN_LEFT + 20f, legendY, itemPaint)

            val dirPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#64748B")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            }
            val directiveText = if (variant.useFamousPieceMode) {
                "Famous Piece: ${variant.famousPieceName.ifBlank { res.musician.famousPiece }} • ${res.notes.size} notes"
            } else {
                "${variant.customDirective.take(55)} • ${res.notes.size} notes"
            }
            canvas.drawText(directiveText, MARGIN_LEFT + 180f, legendY, dirPaint)

            legendY += 13f
        }

        currentY += legendBoxHeight + 16f

        // 3. Staves Systems
        // We render measures across the page. For 4 bars: 2 systems of 2 bars (or 1 system of 4 bars).
        // Let's render 2 bars per system for spacious readability!
        val barsTotal = session.barCount.coerceIn(2, 8)
        val barsPerSystem = 2
        val systemsCount = (barsTotal + barsPerSystem - 1) / barsPerSystem

        val staffLineSpacing = 6f
        val staffHeight = 4 * staffLineSpacing
        val variantSpacing = staffHeight + 22f
        val systemHeight = (results.size * variantSpacing) + 24f

        val staffLeft = MARGIN_LEFT + 75f // Space for clef, meter, and variant label on the left
        val staffRight = PAGE_WIDTH - MARGIN_RIGHT
        val totalStaffWidth = staffRight - staffLeft
        val measureWidth = totalStaffWidth / barsPerSystem

        var systemTop = currentY

        for (sysIdx in 0 until systemsCount) {
            val barStart = sysIdx * barsPerSystem
            val barEnd = (barStart + barsPerSystem - 1).coerceAtMost(barsTotal - 1)

            // Draw Chord symbols above the system
            for (m in 0 until barsPerSystem) {
                val barIndex = barStart + m
                val chord = session.chords.find { it.first == barIndex }?.second?.chordName
                    ?: if (barIndex % 2 == 0) "Am7" else "Dm7"
                val mX = staffLeft + (m * measureWidth)
                val isLastSystem = sysIdx == systemsCount - 1
                canvas.drawText("Bar ${barIndex + 1}: $chord", mX + 6f, systemTop + 10f, chordPaint)
            }

            var staffY = systemTop + 24f

            // Draw staff for each variant in this system
            for ((vIdx, res) in results.withIndex()) {
                val letter = ('A' + vIdx)
                val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#334155")
                    textSize = 9f
                    typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
                // Variant label on left
                canvas.drawText("[$letter] ${res.musician.name.take(10)}", MARGIN_LEFT, staffY + (staffHeight / 2) + 3f, labelPaint)

                // Treble Clef text symbol 𝄞
                val clefPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor("#1E293B")
                    textSize = 24f
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText("𝄞", staffLeft - 22f, staffY + staffHeight - 1f, clefPaint)

                // 5 Staff Lines
                for (l in 0..4) {
                    val ly = staffY + (l * staffLineSpacing)
                    canvas.drawLine(staffLeft - 4f, ly, staffRight, ly, staffLinePaint)
                }

                // Bar separator lines
                canvas.drawLine(staffLeft - 4f, staffY, staffLeft - 4f, staffY + staffHeight, linePaint)
                for (m in 1..barsPerSystem) {
                    val bx = staffLeft + (m * measureWidth)
                    val isFinal = (sysIdx == systemsCount - 1) && (m == barsPerSystem)
                    if (isFinal) {
                        canvas.drawLine(bx - 3f, staffY, bx - 3f, staffY + staffHeight, linePaint)
                        val thickPaint = Paint(linePaint).apply { strokeWidth = 2.2f }
                        canvas.drawLine(bx, staffY, bx, staffY + staffHeight, thickPaint)
                    } else {
                        canvas.drawLine(bx, staffY, bx, staffY + staffHeight, linePaint)
                    }
                }

                // Draw Notes for this variant in these bars
                val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.parseColor(
                        when (vIdx % 4) {
                            0 -> "#0284C7"
                            1 -> "#D97706"
                            2 -> "#059669"
                            else -> "#7C3AED"
                        }
                    )
                    style = Paint.Style.FILL
                }
                val stemPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = notePaint.color
                    strokeWidth = 1.2f
                    style = Paint.Style.STROKE
                }

                val subdivisionsPerBar = (session.beatsPerBar * 4).coerceAtLeast(4)

                for (note in res.notes) {
                    if (note.barIndex !in barStart..barEnd) continue
                    val mOffset = note.barIndex - barStart
                    val noteX = staffLeft + (mOffset * measureWidth) +
                            ((note.subInBar.coerceIn(0, subdivisionsPerBar - 1) + 0.5f) / subdivisionsPerBar.toFloat()) * (measureWidth - 10f) + 4f

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
                    val stepFromB4 = totalDiatonicStep - 34 // B4 is middle line (line 2 from top)
                    val staffCenterY = staffY + (2 * staffLineSpacing)
                    val noteY = staffCenterY - (stepFromB4 * (staffLineSpacing / 2f))

                    // Ledger line for C4 (Middle C) or high notes
                    if (stepFromB4 <= -2 && stepFromB4 % 2 == 0) {
                        canvas.drawLine(noteX - 5f, noteY, noteX + 5f, noteY, staffLinePaint)
                    } else if (stepFromB4 >= 6 && stepFromB4 % 2 == 0) {
                        canvas.drawLine(noteX - 5f, noteY, noteX + 5f, noteY, staffLinePaint)
                    }

                    // Sharp sign
                    if (isSharp) {
                        val sharpPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = notePaint.color
                            textSize = 8.5f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        canvas.drawText("♯", noteX - 7f, noteY + 3f, sharpPaint)
                    }

                    // Notehead (oval)
                    val oval = RectF(noteX - 3.5f, noteY - 2.5f, noteX + 3.5f, noteY + 2.5f)
                    canvas.drawOval(oval, notePaint)

                    // Stem
                    val stemLength = 16f
                    if (stepFromB4 < 0) {
                        // Stem pointing up from right of notehead
                        canvas.drawLine(noteX + 3.5f, noteY, noteX + 3.5f, noteY - stemLength, stemPaint)
                    } else {
                        // Stem pointing down from left of notehead
                        canvas.drawLine(noteX - 3.5f, noteY, noteX - 3.5f, noteY + stemLength, stemPaint)
                    }
                }

                staffY += variantSpacing
            }

            systemTop += systemHeight
        }

        // 4. Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
        val footerY = PAGE_HEIGHT - MARGIN_BOTTOM
        canvas.drawLine(MARGIN_LEFT, footerY - 10f, PAGE_WIDTH - MARGIN_RIGHT, footerY - 10f, linePaint)
        canvas.drawText("Generated by Jamspirate Studio • Sheet Music Comparison Matrix", MARGIN_LEFT, footerY, footerPaint)
        canvas.drawText("Page 1 of 1", PAGE_WIDTH - MARGIN_RIGHT - 45f, footerY, footerPaint)
    }

    /**
     * Opens Android System Share Sheet to send or save the PDF file.
     */
    fun sharePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Jamspirate Sheet Music Comparison Score")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share or Save Sheet Music PDF"))
    }

    /**
     * Triggers Android PrintManager to print directly to WiFi printers or Save as PDF.
     */
    fun printPdf(activity: Activity, pdfFile: File) {
        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("Jamspirate_Score.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                try {
                    FileInputStream(pdfFile).use { input ->
                        FileOutputStream(destination?.fileDescriptor).use { output ->
                            input.copyTo(output)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(
            "Jamspirate Score",
            printAdapter,
            PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                .build()
        )
    }
}
