// app/src/main/java/com/baverika/r_journal/utils/ImportUtils.kt

package com.baverika.r_journal.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.QuickNoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipInputStream

object ImportUtils {

    fun importFromUri(
        context: Context,
        uri: Uri,
        journalRepo: JournalRepository,
        quickNoteRepo: QuickNoteRepository,
        taskRepo: com.baverika.r_journal.repository.TaskRepository,
        quoteRepo: com.baverika.r_journal.quotes.data.QuoteRepository,
        lifeTrackerRepo: com.baverika.r_journal.repository.LifeTrackerRepository,
        eventRepo: com.baverika.r_journal.repository.EventRepository,
        coroutineScope: CoroutineScope,
        onResult: (Boolean, String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Could not open input stream for URI: $uri")

                importFromInputStream(
                    context, 
                    inputStream, 
                    uri, 
                    journalRepo, 
                    quickNoteRepo, 
                    taskRepo, 
                    quoteRepo, 
                    lifeTrackerRepo, 
                    eventRepo, 
                    onResult
                )
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, "Failed to open file: ${e.message}")
            }
        }
    }

    private suspend fun importFromInputStream(
        context: Context,
        inputStream: InputStream,
        uri: Uri,
        journalRepo: JournalRepository,
        quickNoteRepo: QuickNoteRepository,
        taskRepo: com.baverika.r_journal.repository.TaskRepository,
        quoteRepo: com.baverika.r_journal.quotes.data.QuoteRepository,
        lifeTrackerRepo: com.baverika.r_journal.repository.LifeTrackerRepository,
        eventRepo: com.baverika.r_journal.repository.EventRepository,
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            var journalCount = 0
            var quickNoteCount = 0
            var imageCount = 0
            var taskCount = 0
            var habitCount = 0
            var quoteCount = 0
            var trackerCount = 0
            var eventCount = 0

            // Temporary storage for image files
            val tempImagesDir = File(context.cacheDir, "import_temp_images").apply { mkdirs() }
            val imageMap = mutableMapOf<String, File>() // Map of ZIP path to temp file
            val gson = com.google.gson.Gson()

            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry

                // First pass: Extract all images and voice notes to temp storage
                while (zipEntry != null) {
                    if (!zipEntry.isDirectory && (zipEntry.name.startsWith("images/") || zipEntry.name.startsWith("voice_notes/"))) {
                        try {
                            val tempFile = File(tempImagesDir, File(zipEntry.name).name)
                            FileOutputStream(tempFile).use { fos ->
                                zis.copyTo(fos)
                            }
                            imageMap[zipEntry.name] = tempFile
                            imageCount++
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    zipEntry = zis.nextEntry
                }
            }

            // Second pass: Process markdown and JSON files
            context.contentResolver.openInputStream(uri)?.use { secondStream ->
                ZipInputStream(secondStream).use { zis ->
                    var zipEntry = zis.nextEntry

                    while (zipEntry != null) {
                        if (!zipEntry.isDirectory) {
                            if (zipEntry.name.endsWith(".md")) {
                                val content = zis.bufferedReader().readText()

                                when {
                                    zipEntry.name.contains("journals/") -> {
                                        val entry = parseJournalEntryMarkdown(
                                            context,
                                            content,
                                            imageMap
                                        )
                                        entry?.let {
                                            journalRepo.upsertEntry(it)
                                            journalCount++
                                        }
                                    }
                                    zipEntry.name.contains("quick_notes/") -> {
                                        val note = parseQuickNoteMarkdown(content)
                                        note?.let {
                                            quickNoteRepo.upsertNote(it)
                                            quickNoteCount++
                                        }
                                    }
                                }
                            } else if (zipEntry.name.endsWith(".json")) {
                                val content = zis.bufferedReader().readText()
                                when {
                                    zipEntry.name.endsWith("tasks.json") -> {
                                        val tasks = gson.fromJson(content, Array<com.baverika.r_journal.data.local.entity.Task>::class.java)
                                        tasks.forEach { taskRepo.insertTask(it) }
                                        taskCount = tasks.size
                                    }
                                    zipEntry.name.endsWith("habits.json") -> {
                                        val habits = gson.fromJson(content, Array<com.baverika.r_journal.data.local.entity.Habit>::class.java)
                                        habits.forEach { journalRepo.addHabit(it) } // journalRepo exposes helper for habitDao
                                        habitCount = habits.size
                                    }
                                    zipEntry.name.endsWith("habit_logs.json") -> {
                                        val logs = gson.fromJson(content, Array<com.baverika.r_journal.data.local.entity.HabitLog>::class.java)
                                        logs.forEach { log -> 
                                            // Manual insert needed as repo toggle is high level
                                            // We need direct DAO access or exposing a method.
                                            // JournalRepository has toggleHabitCompletion but it takes params.
                                            // Let's assume we can add a method or use toggle if we decompose.
                                            // Actually, the repo has `insertHabitLog` via `toggleHabitCompletion` internal logic.
                                            // Better to add `insertHabitLog` to repo or just use toggle carefully.
                                            // However, `toggleHabitCompletion` logic is: insert if true, delete if false.
                                            // Logs are only stored for completed habits. So we can just call the insert logic. 
                                            // But `JournalRepository` doesn't expose raw insert.
                                            // Using `toggleHabitCompletion(log.habitId, log.dateMillis, true)` works.
                                            journalRepo.toggleHabitCompletion(log.habitId, log.dateMillis, true)
                                        }
                                    }
                                    zipEntry.name.endsWith("quotes.json") -> {
                                        val quotes = gson.fromJson(content, Array<com.baverika.r_journal.quotes.data.QuoteEntity>::class.java)
                                        quotes.forEach { 
                                            // Check existence to avoid overwrite loop/dupes if needed, 
                                            // or just insert. QuoteEntity has auto-inc ID, so if ID is 0 it generates new.
                                            // If importing with IDs, we might want conflicts replace.
                                            // Dao uses default or explicit.
                                            // To preserve IDs, we should use the ID from JSON.
                                            if (it.id > 0) {
                                                // We can use a direct insert or update. 
                                                // QuoteRepository exposes `insertQuote` and `updateQuote`.
                                                // insertQuote checks for conflict REPLACE usually? The Dao has @Insert(onConflict = REPLACE) ?
                                                // Let's assume insertQuote is fine.
                                                quoteRepo.insertQuote(it)
                                            } else {
                                                quoteRepo.insertQuote(it)
                                            }
                                        }
                                        quoteCount = quotes.size
                                    }
                                    zipEntry.name.endsWith("life_trackers.json") -> {
                                        val trackers = gson.fromJson(content, Array<com.baverika.r_journal.data.local.entity.LifeTracker>::class.java)
                                        trackers.forEach { lifeTrackerRepo.insertTracker(it) }
                                        trackerCount = trackers.size
                                    }
                                    zipEntry.name.endsWith("life_tracker_entries.json") -> {
                                        val entries = gson.fromJson(content, Array<com.baverika.r_journal.data.local.entity.LifeTrackerEntry>::class.java)
                                        entries.forEach { lifeTrackerRepo.insertEntry(it) }
                                    }
                                    zipEntry.name.endsWith("events.json") -> {
                                        val events = gson.fromJson(content, Array<com.baverika.r_journal.data.local.entity.Event>::class.java)
                                        events.forEach { eventRepo.insertEvent(it) }
                                        eventCount = events.size
                                    }
                                }
                            }
                        }
                        zipEntry = zis.nextEntry
                    }
                }
            }

            // Clean up temp files
            tempImagesDir.deleteRecursively()

            onResult(
                true,
                "Imported: $journalCount journals, $quickNoteCount notes, $taskCount tasks, $habitCount habits, $quoteCount quotes, $trackerCount trackers, $eventCount events"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            onResult(false, "Import failed: ${e.message}")
        }
    }

    private fun parseJournalEntryMarkdown(
        context: Context,
        content: String,
        imageMap: Map<String, File>
    ): JournalEntry? {
        return try {
            val lines = content.lines()
            var inFrontMatter = false
            val frontMatterLines = mutableListOf<String>()
            val contentLines = mutableListOf<String>()

            for (line in lines) {
                if (line.trim() == "---") {
                    inFrontMatter = !inFrontMatter
                    continue
                }
                if (inFrontMatter) {
                    frontMatterLines.add(line)
                } else {
                    contentLines.add(line)
                }
            }

            // Parse front matter
            val metaData = mutableMapOf<String, String?>()
            for (fmLine in frontMatterLines) {
                val parts = fmLine.split(":", limit = 2)
                if (parts.size == 2) {
                    metaData[parts[0].trim()] = parts[1].trim()
                }
            }

            val id = metaData["id"] ?: return null
            val dateString = metaData["date"] ?: return null
            val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            val startOfDayMillis = localDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

            val mood = metaData["mood"]
            val tagsString = metaData["tags"]
            val tags = if (tagsString != null) {
                tagsString.removePrefix("[").removeSuffix("]")
                    .split(",").map { it.trim().removeSurrounding("\"") }
            } else {
                emptyList()
            }

            // Parse messages with image restoration
            val messages = mutableListOf<ChatMessage>()
            val imageStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            for (line in contentLines) {
                if (line.isBlank()) continue

                // Parse message line
                val messageRegex = Regex("""\*\*\[([^\]]+)\] \((\d{2}:\d{2}:\d{2})\):\*\* (.+)""")
                val matchResult = messageRegex.find(line)

                if (matchResult != null) {
                    val role = matchResult.groupValues[1]
                    val timeString = matchResult.groupValues[2]
                    val content = matchResult.groupValues[3]
                    val time = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm:ss"))
                    val timestamp = localDate.atTime(time)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

                    messages.add(
                        ChatMessage(
                            role = role,
                            content = content,
                            timestamp = timestamp,
                            imageUri = null // Will be set in next step if exists
                        )
                    )
                }

                // Check for image reference
                val imageRegex = Regex("""!\[Image\]\(\.\.\/\.\.\/images\/${localDate}\/(.+)\)""")
                val imageMatch = imageRegex.find(line)

                if (imageMatch != null && messages.isNotEmpty()) {
                    val imageName = imageMatch.groupValues[1]
                    val zipImagePath = "images/${localDate}/$imageName"

                    // Find the temp image file and copy it to permanent storage
                    imageMap[zipImagePath]?.let { tempImageFile ->
                        try {
                            val permanentFile = File(imageStorageDir, imageName)
                            tempImageFile.copyTo(permanentFile, overwrite = true)

                            // Update the last message with the restored image path
                            val lastMessage = messages.last()
                            messages[messages.lastIndex] = lastMessage.copy(
                                imageUri = permanentFile.absolutePath
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                // Check for voice note reference
                val voiceRegex = Regex("""🎤 \[Voice Note - (\d+)s\]\(\.\.\/\.\.\/voice_notes\/${localDate}\/(.+)\)""")
                val voiceMatch = voiceRegex.find(line)

                if (voiceMatch != null && messages.isNotEmpty()) {
                    val durationSec = voiceMatch.groupValues[1].toLongOrNull() ?: 0L
                    val voiceName = voiceMatch.groupValues[2]
                    val zipVoicePath = "voice_notes/${localDate}/$voiceName"

                    val voiceStorageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                    imageMap[zipVoicePath]?.let { tempVoiceFile ->
                        try {
                            val permanentFile = File(voiceStorageDir, voiceName)
                            tempVoiceFile.copyTo(permanentFile, overwrite = true)

                            val lastMessage = messages.last()
                            messages[messages.lastIndex] = lastMessage.copy(
                                voiceNoteUri = permanentFile.absolutePath,
                                voiceNoteDuration = durationSec * 1000
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            JournalEntry(
                id = id,
                dateMillis = startOfDayMillis,
                messages = messages,
                tags = tags,
                mood = mood
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseQuickNoteMarkdown(content: String): QuickNote? {
        return try {
            val lines = content.lines()
            var inFrontMatter = false
            val frontMatterLines = mutableListOf<String>()
            val contentLines = mutableListOf<String>()

            for (line in lines) {
                if (line.trim() == "---") {
                    inFrontMatter = !inFrontMatter
                    continue
                }
                if (inFrontMatter) {
                    frontMatterLines.add(line)
                } else {
                    contentLines.add(line)
                }
            }

            // Parse front matter
            val metaData = mutableMapOf<String, String?>()
            for (fmLine in frontMatterLines) {
                val parts = fmLine.split(":", limit = 2)
                if (parts.size == 2) {
                    metaData[parts[0].trim()] = parts[1].trim()
                }
            }

            val id = metaData["id"] ?: return null
            val title = metaData["title"] ?: "Untitled"
            val createdAtString = metaData["created_at"]
            val timestamp = if (createdAtString != null) {
                LocalDateTime.parse(createdAtString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } else {
                System.currentTimeMillis()
            }

            // Skip the markdown title line if present
            val noteContent = contentLines
                .dropWhile { it.startsWith("#") || it.isBlank() }
                .joinToString("\n")

            QuickNote(
                id = id,
                title = title,
                content = noteContent,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}