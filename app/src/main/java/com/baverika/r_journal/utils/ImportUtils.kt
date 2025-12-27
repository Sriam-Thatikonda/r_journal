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
        coroutineScope: CoroutineScope,
        onResult: (Boolean, String) -> Unit
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: throw Exception("Could not open input stream for URI: $uri")

                importFromInputStream(context, inputStream, uri, journalRepo, quickNoteRepo, onResult)
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
        onResult: (Boolean, String) -> Unit
    ) {
        try {
            var journalCount = 0
            var quickNoteCount = 0
            var imageCount = 0

            // Temporary storage for image files
            val tempImagesDir = File(context.cacheDir, "import_temp_images").apply { mkdirs() }
            val imageMap = mutableMapOf<String, File>() // Map of ZIP path to temp file

            ZipInputStream(inputStream).use { zis ->
                var zipEntry = zis.nextEntry

                // First pass: Extract all images to temp storage
                while (zipEntry != null) {
                    if (!zipEntry.isDirectory && zipEntry.name.startsWith("images/")) {
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

            // Second pass: Process markdown files and restore images
            context.contentResolver.openInputStream(uri)?.use { secondStream ->
                ZipInputStream(secondStream).use { zis ->
                    var zipEntry = zis.nextEntry

                    while (zipEntry != null) {
                        if (!zipEntry.isDirectory && zipEntry.name.endsWith(".md")) {
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
                        }
                        zipEntry = zis.nextEntry
                    }
                }
            }

            // Clean up temp files
            tempImagesDir.deleteRecursively()

            onResult(
                true,
                "Successfully imported $journalCount journals, $quickNoteCount notes, and $imageCount images"
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