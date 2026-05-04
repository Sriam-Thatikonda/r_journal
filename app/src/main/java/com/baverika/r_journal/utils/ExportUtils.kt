// app/src/main/java/com/baverika/r_journal/utils/ExportUtils.kt

package com.baverika.r_journal.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.data.local.entity.Password
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
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    fun exportAll(
        context: Context,
        journals: List<JournalEntry>,
        quickNotes: List<QuickNote>,
        taskCategories: List<com.baverika.r_journal.data.local.entity.TaskCategory>,
        tasks: List<com.baverika.r_journal.data.local.entity.Task>,
        habits: List<com.baverika.r_journal.data.local.entity.Habit>,
        habitLogs: List<com.baverika.r_journal.data.local.entity.HabitLog>,
        quotes: List<com.baverika.r_journal.quotes.data.QuoteEntity>,
        lifeTrackers: List<com.baverika.r_journal.data.local.entity.LifeTracker>,
        lifeTrackerEntries: List<com.baverika.r_journal.data.local.entity.LifeTrackerEntry>,

        events: List<com.baverika.r_journal.data.local.entity.Event>,
        passwords: List<Password>
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

            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()

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

                        // Export voice notes
                        message.voiceNoteUri?.let { voicePath ->
                            val voiceFile = File(voicePath)
                            if (voiceFile.exists()) {
                                try {
                                    val voiceFileName = "voice_notes/${entry.localDate}/${voiceFile.name}"
                                    zos.putNextEntry(ZipEntry(voiceFileName))
                                    FileInputStream(voiceFile).use { fis ->
                                        fis.copyTo(zos)
                                    }
                                    zos.closeEntry()
                                } catch (e: Exception) {
                                    e.printStackTrace()
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

                // Export Task Categories (JSON) — MUST be exported before tasks (FK dependency)
                if (taskCategories.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/task_categories.json"))
                    zos.write(gson.toJson(taskCategories).toByteArray())
                    zos.closeEntry()
                }

                // Export Tasks (JSON)
                if (tasks.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/tasks.json"))
                    zos.write(gson.toJson(tasks).toByteArray())
                    zos.closeEntry()
                }

                // Export Habits (JSON)
                if (habits.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/habits.json"))
                    zos.write(gson.toJson(habits).toByteArray())
                    zos.closeEntry()
                }

                // Export Habit Logs (JSON)
                if (habitLogs.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/habit_logs.json"))
                    zos.write(gson.toJson(habitLogs).toByteArray())
                    zos.closeEntry()
                }

                // Export Quotes (JSON)
                if (quotes.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/quotes.json"))
                    zos.write(gson.toJson(quotes).toByteArray())
                    zos.closeEntry()
                }

                // Export Life Trackers (JSON)
                if (lifeTrackers.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/life_trackers.json"))
                    zos.write(gson.toJson(lifeTrackers).toByteArray())
                    zos.closeEntry()
                }

                // Export Life Tracker Entries (JSON)
                if (lifeTrackerEntries.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/life_tracker_entries.json"))
                    zos.write(gson.toJson(lifeTrackerEntries).toByteArray())
                    zos.closeEntry()
                }

                // Export Events (JSON)
                if (events.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/events.json"))
                    zos.write(gson.toJson(events).toByteArray())
                    zos.closeEntry()
                }

                // Export Passwords (JSON) - DECRYPTED for portability
                if (passwords.isNotEmpty()) {
                    try {
                        // We must decrypt for the export to be useful on other devices
                        // WARNING: This puts plain text passwords in the JSON
                        val exportablePasswords = passwords.map { 
                            // Try to decrypt; if fails, keep original (might already be plain or broken)
                            val decoded = try {
                                SecurityUtils.decrypt(it.passwordValue)
                            } catch (e: Exception) {
                                it.passwordValue
                            }
                            it.copy(passwordValue = decoded)
                        }
                        zos.putNextEntry(ZipEntry("data/passwords.json"))
                        zos.write(gson.toJson(exportablePasswords).toByteArray())
                        zos.closeEntry()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // Add a README file
                val readme = buildReadme(
                    journals.size, 
                    quickNotes.size, 
                    tasks.size, 
                    habits.size, 
                    quotes.size,
                    lifeTrackers.size,

                    events.size,
                    passwords.size
                )
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

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
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

                if (message.content.isNotBlank()) {
                    append("**[${message.role}] ($time):** ${message.content}\n")
                } else if (message.voiceNoteUri != null) {
                    append("**[${message.role}] ($time):** 🎤 Voice Note\n")
                } else if (message.imageUri != null) {
                    append("**[${message.role}] ($time):**\n")
                }

                message.imageUri?.let { imagePath ->
                    val imageFile = File(imagePath)
                    if (imageFile.exists()) {
                        append("![Image](../../images/${entry.localDate}/${imageFile.name})\n")
                    }
                }

                message.voiceNoteUri?.let { voicePath ->
                    val voiceFile = File(voicePath)
                    if (voiceFile.exists()) {
                        val durationSec = message.voiceNoteDuration / 1000
                        append("🎤 [Voice Note - ${durationSec}s](../../voice_notes/${entry.localDate}/${voiceFile.name})\n")
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

    private fun buildReadme(
        journalCount: Int, 
        noteCount: Int, 
        taskCount: Int, 
        habitCount: Int, 
        quoteCount: Int,
        trackerCount: Int,

        eventCount: Int,
        passwordCount: Int
    ): String {
        return """
            R-Journal Export
            ================
            
            Export Date: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}
            
            Contents:
            - $journalCount journal entries
            - $noteCount quick notes
            - $taskCount tasks
            - $habitCount habits
            - $quoteCount quotes
            - $trackerCount life trackers

            - $eventCount special dates/events
            - $passwordCount passwords (CAUTION: Exported unencrypted inside this archive)
            
            Structure:
            /data
              /journals             - Contains all journal entries as Markdown files
              /quick_notes          - Contains all quick notes as Markdown files
              tasks.json            - All tasks in JSON format
              habits.json           - All habits in JSON format
              habit_logs.json       - All habit logs/completions in JSON format
              quotes.json           - All quotes in JSON format
              life_trackers.json    - All life trackers in JSON format
              life_tracker_entries.json - All life tracker entries in JSON format

              events.json           - All special dates/events in JSON format
              passwords.json        - All saved passwords in JSON format
            /images
              /[date]               - Images organized by journal entry date
            
            To import this data back into R-Journal:
            1. Open R-Journal app
            2. Navigate to Import from the menu
            3. Select this ZIP file
            
            Note: Keep this ZIP file safe as it contains all your journal data!
        """.trimIndent()
    }
}