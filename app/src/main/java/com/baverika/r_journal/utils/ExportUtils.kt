// app/src/main/java/com/baverika/r_journal/utils/ExportUtils.kt

package com.baverika.r_journal.utils

import android.content.Context
import android.os.Environment
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.QuickNote
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ExportUtils {

    /**
     * Exports all journal entries and quick notes to a single organized ZIP file.
     * Includes images in organized folders.
     */
    fun exportAll(
        context: Context,
        journals: List<JournalEntry>,
        quickNotes: List<QuickNote>
    ): Pair<Boolean, String?> {
        return try {
            // 1. Determine export directory
            val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // 2. Create unique filename
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val zipFileName = "r_journal_export_$timestamp.zip"
            val zipFile = File(exportDir, zipFileName)

            // 3. Create ZIP and write content
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                // Export journal entries
                journals.forEach { entry ->
                    // Write journal entry markdown
                    val fileName = "data/journals/journal_${entry.localDate}_${entry.id.take(8)}.md"
                    val content = buildJournalEntryMarkdown(entry)
                    zos.putNextEntry(ZipEntry(fileName))
                    zos.write(content.toByteArray())
                    zos.closeEntry()

                    // Export images for this entry
                    entry.messages.forEach { message ->
                        message.imageUri?.let { imagePath ->
                            val imageFile = File(imagePath)
                            if (imageFile.exists()) {
                                try {
                                    // Organize by entry date
                                    val imageFileName = "images/${entry.localDate}/${imageFile.name}"
                                    zos.putNextEntry(ZipEntry(imageFileName))
                                    FileInputStream(imageFile).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    // Continue with other images even if one fails
                                }
                            }
                        }
                    }
                }

                // Export quick notes
                quickNotes.forEach { note ->
                    val fileName = "data/quick_notes/note_${note.id.take(8)}.md"
                    val content = buildQuickNoteMarkdown(note)
                    zos.putNextEntry(ZipEntry(fileName))
                    zos.write(content.toByteArray())
                    zos.closeEntry()
                }

                // Add a README file
                val readme = buildReadme(journals.size, quickNotes.size)
                zos.putNextEntry(ZipEntry("README.txt"))
                zos.write(readme.toByteArray())
                zos.closeEntry()
            }

            Pair(true, "Exported to: ${zipFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Export failed: ${e.message}")
        }
    }

    private fun buildJournalEntryMarkdown(entry: JournalEntry): String {
        return buildString {
            append("---\n")
            append("date: ${entry.localDate}\n")
            append("id: ${entry.id}\n")
            entry.mood?.let { append("mood: $it\n") }
            if (entry.tags.isNotEmpty()) {
                append("tags: [${entry.tags.joinToString(", ")}]\n")
            }
            append("---\n\n")

            append("# ${entry.localDate}\n\n")

            entry.messages.forEach { message ->
                val time = LocalDateTime
                    .ofInstant(java.time.Instant.ofEpochMilli(message.timestamp), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                append("**[${message.role}] ($time):** ${message.content}\n")

                message.imageUri?.let { imagePath ->
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        append("![Image](../../images/${entry.localDate}/${imageFile.name})\n")
                    }
                }
                append("\n")
            }
        }
    }

    private fun buildQuickNoteMarkdown(note: QuickNote): String {
        return buildString {
            append("---\n")
            append("title: ${note.title}\n")
            append("id: ${note.id}\n")
            val createdAt = LocalDateTime
                .ofInstant(java.time.Instant.ofEpochMilli(note.timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            append("created_at: $createdAt\n")
            append("---\n\n")
            append("# ${note.title}\n\n")
            append(note.content)
        }
    }

    private fun buildReadme(journalCount: Int, noteCount: Int): String {
        return """
            R-Journal Export
            ================
            
            Export Date: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
            
            Contents:
            - $journalCount journal entries
            - $noteCount quick notes
            
            Structure:
            /data
              /journals  - Contains all journal entries as Markdown files
              /quick_notes - Contains all quick notes as Markdown files
            /images
              /[date]  - Images organized by journal entry date
            
            To import this data back into R-Journal:
            1. Open R-Journal app
            2. Navigate to Import from the menu
            3. Select this ZIP file
            
            Note: Keep this ZIP file safe as it contains all your journal data!
        """.trimIndent()
    }
}