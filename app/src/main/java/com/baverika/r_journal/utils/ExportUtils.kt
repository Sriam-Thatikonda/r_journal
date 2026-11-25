// app/src/main/java/com/baverika/r_journal/utils/ExportUtils.kt

package com.baverika.r_journal.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.data.local.entity.Password
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializer
import com.google.gson.JsonPrimitive
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
        notes: List<QuickNote>,
        taskCategories: List<com.baverika.r_journal.data.local.entity.TaskCategory>,
        tasks: List<com.baverika.r_journal.data.local.entity.Task>,
        habits: List<com.baverika.r_journal.data.local.entity.Habit>,
        habitLogs: List<com.baverika.r_journal.data.local.entity.HabitLog>,
        quotes: List<com.baverika.r_journal.quotes.data.QuoteEntity>,
        lifeTrackers: List<com.baverika.r_journal.data.local.entity.LifeTracker>,
        lifeTrackerEntries: List<com.baverika.r_journal.data.local.entity.LifeTrackerEntry>,
        events: List<com.baverika.r_journal.data.local.entity.Event>,
        passwords: List<Password>,
        trackers: List<com.baverika.r_journal.data.local.entity.Tracker>,
        trackerHistory: List<com.baverika.r_journal.data.local.entity.TrackerHistory>,
        challenges: List<com.baverika.r_journal.data.ChallengeEntity>,
        mineEntry: JournalEntry? = null,
        vaultPassphrase: String? = null
    ): Pair<Boolean, String?> {
        return try {
            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val zipFileName = "r_journal_export_$timestamp.zip"

            val gson = com.google.gson.GsonBuilder()
                .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ ->
                    JsonPrimitive(src.toString())
                })
                .registerTypeAdapter(LocalDate::class.java, JsonDeserializer<LocalDate> { json, _, _ ->
                    LocalDate.parse(json.asString)
                })
                .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ ->
                    JsonPrimitive(src.toString())
                })
                .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer<LocalDateTime> { json, _, _ ->
                    LocalDateTime.parse(json.asString)
                })
                .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ ->
                    JsonPrimitive(src.toString())
                })
                .registerTypeAdapter(LocalTime::class.java, JsonDeserializer<LocalTime> { json, _, _ ->
                    LocalTime.parse(json.asString)
                })
                .setPrettyPrinting()
                .create()

            val writeZip: (OutputStream) -> Unit = { outputStream ->
                ZipOutputStream(outputStream).use { zos ->
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

                // Export Mine Journal (JSON)
                mineEntry?.let { mine ->
                    if (mine.messages.isNotEmpty()) {
                        zos.putNextEntry(ZipEntry("data/mine_journal.json"))
                        zos.write(gson.toJson(mine).toByteArray())
                        zos.closeEntry()

                        mine.messages.forEach { message ->
                            message.imageUri?.let { imagePath ->
                                val imageFile = File(imagePath)
                                if (imageFile.exists()) {
                                    try {
                                        val imageFileName = "images/mine/${imageFile.name}"
                                        zos.putNextEntry(ZipEntry(imageFileName))
                                        FileInputStream(imageFile).use { fis -> fis.copyTo(zos) }
                                        zos.closeEntry()
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }

                            message.voiceNoteUri?.let { voicePath ->
                                val voiceFile = File(voicePath)
                                if (voiceFile.exists()) {
                                    try {
                                        val voiceFileName = "voice_notes/mine/${voiceFile.name}"
                                        zos.putNextEntry(ZipEntry(voiceFileName))
                                        FileInputStream(voiceFile).use { fis -> fis.copyTo(zos) }
                                        zos.closeEntry()
                                    } catch (e: Exception) { e.printStackTrace() }
                                }
                            }
                        }
                    }
                }

                // Export quick notes
                notes.forEach { note ->
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

                // Export Passwords (JSON) - encrypted with passphrase when provided
                val isVaultEncrypted = !vaultPassphrase.isNullOrBlank()
                if (passwords.isNotEmpty()) {
                    try {
                        val exportablePasswords = passwords.map { password ->
                            val decoded = try {
                                SecurityUtils.decrypt(password.passwordValue)
                            } catch (e: Exception) {
                                password.passwordValue
                            }
                            val decodedHistory = password.history.map { hItem ->
                                val hDecoded = try {
                                    SecurityUtils.decrypt(hItem.passwordValue)
                                } catch (e: Exception) {
                                    hItem.passwordValue
                                }
                                hItem.copy(passwordValue = hDecoded)
                            }
                            password.copy(passwordValue = decoded, history = decodedHistory)
                        }
                        val passwordsJson = gson.toJson(exportablePasswords)
                        if (isVaultEncrypted) {
                            val encryptedBlob = SecurityUtils.encryptWithPassphrase(passwordsJson, vaultPassphrase!!)
                            zos.putNextEntry(ZipEntry("data/passwords.enc.json"))
                            zos.write(encryptedBlob.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        } else {
                            zos.putNextEntry(ZipEntry("data/passwords.json"))
                            zos.write(passwordsJson.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        throw e
                    }
                }


                // Export Trackers (JSON)
                if (trackers.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/trackers.json"))
                    zos.write(gson.toJson(trackers).toByteArray())
                    zos.closeEntry()
                }

                // Export Tracker History (JSON)
                if (trackerHistory.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/tracker_history.json"))
                    zos.write(gson.toJson(trackerHistory).toByteArray())
                    zos.closeEntry()
                }

                // Export Challenges (JSON)
                if (challenges.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("data/challenges.json"))
                    zos.write(gson.toJson(challenges).toByteArray())
                    zos.closeEntry()
                }

                // Add a README file
                val readme = buildReadme(
                    journals.size,
                    notes.size,
                    tasks.size,
                    habits.size,
                    quotes.size,
                    lifeTrackers.size,
                    events.size,
                    passwords.size,
                    trackers.size,
                    trackerHistory.size,
                    challenges.size,
                    isVaultEncrypted
                )
                zos.putNextEntry(ZipEntry("README.txt"))
                zos.write(readme.toByteArray())
                zos.closeEntry()
                }
            }

            val savedLocation: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, zipFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IllegalStateException("Failed to create Downloads entry")
                try {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        writeZip(out)
                    } ?: throw IllegalStateException("Failed to open Downloads output stream")
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(itemUri, contentValues, null, null)
                    "Downloads/$zipFileName"
                } catch (e: Exception) {
                    resolver.delete(itemUri, null, null)
                    throw e
                }
            } else {
                val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!exportDir.exists()) exportDir.mkdirs()
                val zipFile = File(exportDir, zipFileName)
                zipFile.outputStream().use { writeZip(it) }
                zipFile.absolutePath
            }

            Pair(true, "Exported to: $savedLocation")
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
            append(com.baverika.r_journal.data.model.RichContent.fromContentString(note.content).toPlainText())
        }
    }

    private fun buildReadme(
        journalCount: Int, 
        noteCount: Int, 
        taskCount: Int, 
        habitCount: Int, 
        quoteCount: Int,
        lifeTrackerCount: Int,
        eventCount: Int,
        passwordCount: Int,
        trackerCount: Int,
        trackerHistoryCount: Int,
        challengeCount: Int,
        isVaultEncrypted: Boolean
    ): String {
        val vaultNote = if (isVaultEncrypted) {
            "Encrypted with backup passphrase (passwords.enc.json)"
        } else {
            "CAUTION: Exported unencrypted inside this archive (passwords.json)"
        }
        val vaultFileLine = if (isVaultEncrypted) {
            "passwords.enc.json    - Passphrase-encrypted saved passwords"
        } else {
            "passwords.json        - All saved passwords in JSON format"
        }
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
            - $lifeTrackerCount life trackers
            - $trackerCount trackers
            - $trackerHistoryCount tracker history records
            - $challengeCount challenges
            - $eventCount special dates/events
            - $passwordCount passwords ($vaultNote)
            
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
              trackers.json         - All trackers in JSON format
              tracker_history.json  - All tracker history records in JSON format
              challenges.json       - All challenges in JSON format
              events.json           - All special dates/events in JSON format
              $vaultFileLine
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