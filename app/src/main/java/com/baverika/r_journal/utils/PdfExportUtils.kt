package com.baverika.r_journal.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.StaticLayout
import android.text.TextPaint
import android.text.Layout
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.QuickNote
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object PdfExportUtils {

    fun exportToPdf(
        context: Context,
        journals: List<JournalEntry>,
        quickNotes: List<QuickNote>
    ): Pair<Boolean, String?> {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (approx)

        try {
            // Setup Paints
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val bodyPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.DEFAULT
            }
            val metaPaint = TextPaint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = Typeface.DEFAULT
            }

            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var yPosition = 50f
            val margin = 50f
            val contentWidth = pageInfo.pageWidth - (2 * margin)

            // --- Title Page ---
            canvas.drawText("My Journal", margin, yPosition, titlePaint)
            yPosition += 40f
            canvas.drawText("Exported on: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}", margin, yPosition, metaPaint)
            yPosition += 60f

            // --- Journal Entries ---
            if (journals.isNotEmpty()) {
                canvas.drawText("Journal Entries", margin, yPosition, headerPaint)
                yPosition += 30f

                journals.sortedByDescending { it.dateMillis }.forEach { entry ->
                    val dateStr = entry.localDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
                    val moodStr = entry.mood?.let { "Mood: $it" } ?: ""
                    
                    // Measure Date Header
                    val dateLayout = StaticLayout.Builder.obtain(dateStr, 0, dateStr.length, headerPaint, contentWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build()
                    
                    // Measure Content
                    val contentText = entry.messages.joinToString("\n\n") { msg ->
                        val time = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(msg.timestamp), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                        "[$time] ${msg.content}"
                    }
                    val contentLayout = StaticLayout.Builder.obtain(contentText, 0, contentText.length, bodyPaint, contentWidth.toInt())
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .build()

                    val entryHeight = dateLayout.height + contentLayout.height + 60f // + spacing

                    // Check if we need a new page
                    if (yPosition + entryHeight > pageInfo.pageHeight - margin) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = margin
                    }

                    // Draw Entry
                    dateLayout.draw(canvas, margin, yPosition)
                    yPosition += dateLayout.height + 5f

                    if (moodStr.isNotEmpty()) {
                         canvas.drawText(moodStr, margin, yPosition, metaPaint)
                         yPosition += 15f
                    }
                    
                    yPosition += 10f
                    contentLayout.draw(canvas, margin, yPosition)
                    yPosition += contentLayout.height + 30f
                    
                    // Divider
                    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
                    canvas.drawLine(margin, yPosition - 15f, pageInfo.pageWidth - margin, yPosition - 15f, linePaint)
                }
            }
            
            // New Page for Quick Notes if needed
            if (yPosition > pageInfo.pageHeight - 200) {
                 document.finishPage(page)
                 page = document.startPage(pageInfo)
                 canvas = page.canvas
                 yPosition = margin
            } else {
                yPosition += 40f
            }

            if (quickNotes.isNotEmpty()) {
                canvas.drawText("Notes", margin, yPosition, headerPaint)
                yPosition += 30f

                quickNotes.sortedByDescending { it.timestamp }.forEach { note ->
                    val titleStr = note.title
                    val dateStr = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(note.timestamp), ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm"))
                    
                    val titleLayout = StaticLayout.Builder.obtain(titleStr, 0, titleStr.length, headerPaint, contentWidth.toInt()).build()
                    val contentLayout = StaticLayout.Builder.obtain(note.content, 0, note.content.length, bodyPaint, contentWidth.toInt()).build()
                    
                    val noteHeight = titleLayout.height + contentLayout.height + 50f

                    if (yPosition + noteHeight > pageInfo.pageHeight - margin) {
                        document.finishPage(page)
                        page = document.startPage(pageInfo)
                        canvas = page.canvas
                        yPosition = margin
                    }

                    titleLayout.draw(canvas, margin, yPosition)
                    yPosition += titleLayout.height + 5f
                    
                    canvas.drawText(dateStr, margin, yPosition, metaPaint)
                    yPosition += 15f
                    
                    contentLayout.draw(canvas, margin, yPosition)
                    yPosition += contentLayout.height + 30f
                    
                    val linePaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 1f }
                    canvas.drawLine(margin, yPosition - 15f, pageInfo.pageWidth - margin, yPosition - 15f, linePaint)
                }
            }

            document.finishPage(page)

            // Save File
            val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!exportDir.exists()) exportDir.mkdirs()
            
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "My_Journal_Book_$timestamp.pdf"
            val file = File(exportDir, fileName)

            document.writeTo(FileOutputStream(file))
            
            return Pair(true, "Saved to Downloads: $fileName")

        } catch (e: Exception) {
            e.printStackTrace()
            return Pair(false, "PDF Export failed: ${e.message}")
        } finally {
            document.close()
        }
    }
    
    // Helper extension to draw StaticLayout at x,y
    private fun StaticLayout.draw(canvas: Canvas, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        this.draw(canvas)
        canvas.restore()
    }
}
