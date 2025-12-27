// app/src/main/java/com/baverika/r_journal/ui/viewmodel/JournalViewModel.kt

package com.baverika.r_journal.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.repository.EventRepository
import com.baverika.r_journal.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.util.*

class JournalViewModel(
    private val repo: JournalRepository,
    private val eventRepo: EventRepository,
    context: Context
) : ViewModel() {

    // Use application context to avoid memory leaks
    private val appContext = context.applicationContext

    // State for the currently loaded/active entry
    var currentEntry by mutableStateOf(JournalEntry.createForToday())
        private set

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Events for the current entry's date
    private val _todaysEvents = MutableStateFlow<List<Event>>(emptyList())
    val todaysEvents: StateFlow<List<Event>> = _todaysEvents.asStateFlow()

    // Track if current entry is today
    val isCurrentEntryToday: Boolean
        get() = JournalEntry.isToday(currentEntry.dateMillis)

    // Track if mood can be edited (Allowed for all entries now)
    val canEditMood: Boolean
        get() = true

    init {
        loadTodaysEntry()
    }

    fun loadTodaysEntry() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1) get local
                val local = repo.getOrCreateTodaysEntry()
                currentEntry = local

                Log.d("VM", "Loaded entry messages (LOCAL): " +
                        local.messages.joinToString { "${it.id}:${it.replyToMessageId}:${it.replyPreview}" })

            } finally {
                // ✅ Show local data immediately, don't wait for network
                _isLoading.value = false
            }

            // 2) merge with server (site ➜ app)
            // This happens silently in background while user sees local data
            val merged = repo.syncTodayFromServer(currentEntry)
            if (merged != null) {
                currentEntry = merged

                Log.d("VM", "Loaded entry messages (MERGED): " +
                        merged.messages.joinToString { "${it.id}:${it.replyToMessageId}:${it.replyPreview}" })
            }

            // 3) Load events for today
            loadEventsForDate(currentEntry.dateMillis)
        }
    }

    fun loadEntryForEditing(entryId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val entry = repo.getEntryById(entryId)
                if (entry != null) {
                    currentEntry = entry
                } else {
                    // Entry not found, fallback to today
                    loadTodaysEntry()
                }
                // Load events for the loaded entry's date
                loadEventsForDate(currentEntry.dateMillis)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadEventsForDate(dateMillis: Long) {
        viewModelScope.launch {
            val date = java.time.Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            eventRepo.allEvents.collect { events ->
                _todaysEvents.value = events.filter { event ->
                    event.day == date.dayOfMonth && event.month == date.monthValue
                }
            }
        }
    }

    // Text-only add, supports replying via optional replyTo
    fun addMessage(content: String, replyTo: ChatMessage? = null) {
        addMessageWithImage(content, null, replyTo)
    }

    // Adds a message, optionally with image and optional reply target.
    fun addMessageWithImage(
        content: String,
        imageUri: String?,
        replyTo: ChatMessage? = null    // optional reply target
    ) {
        if (content.isBlank() && imageUri == null) return

        var savedImagePath: String? = null
        imageUri?.let { uriString ->
            try {
                val uri: Uri = Uri.parse(uriString)
                savedImagePath = saveImageToPrivateStorage(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                // Image save failed - add message without image
                savedImagePath = null
            }
        }

        val msg = ChatMessage(
            role = "user",
            content = content,
            timestamp = System.currentTimeMillis(),
            imageUri = savedImagePath,
            // reply metadata
            replyToMessageId = replyTo?.id,
            replyPreview = replyTo?.content?.take(80)
        )

        currentEntry = currentEntry.copy(messages = currentEntry.messages + msg)
        saveCurrentEntry()
    }

    private fun saveImageToPrivateStorage(imageUri: Uri): String? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_$timeStamp.jpg"
            val storageDir: File? = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, fileName)

            // Load and compress the image
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(
                appContext.contentResolver,
                imageUri
            )

            // Calculate scaled dimensions (max 1024px on longest side)
            val maxDimension = 1024
            val scale = if (bitmap.width > bitmap.height) {
                maxDimension.toFloat() / bitmap.width
            } else {
                maxDimension.toFloat() / bitmap.height
            }

            val scaledWidth = (bitmap.width * scale).toInt()
            val scaledHeight = (bitmap.height * scale).toInt()

            // Scale bitmap if needed
            val scaledBitmap = if (scale < 1.0f) {
                android.graphics.Bitmap.createScaledBitmap(
                    bitmap,
                    scaledWidth,
                    scaledHeight,
                    true
                )
            } else {
                bitmap
            }

            // Compress and save
            FileOutputStream(imageFile).use { outputStream ->
                scaledBitmap.compress(
                    android.graphics.Bitmap.CompressFormat.JPEG,
                    85, // Quality 85%
                    outputStream
                )
            }

            // Clean up bitmaps
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()

            imageFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun editMessage(messageId: String, newContent: String) {
        // Find target message
        val message = currentEntry.messages.find { it.id == messageId } ?: return

        // Only allow editing messages from today
        if (!isMessageFromToday(message)) return

        if (newContent.isBlank()) {
            // If new content empty -> delete
            deleteMessage(messageId)
            return
        }

        // Update target message content
        val updatedMessages = currentEntry.messages.map { msg ->
            if (msg.id == messageId) {
                // replace content and timestamp
                msg.copy(content = newContent, timestamp = System.currentTimeMillis())
            } else {
                msg
            }
        }.map { msg ->
            // If any message references this edited message via replyToMessageId,
            // update their replyPreview to reflect the new content (keep same length truncation)
            if (msg.replyToMessageId == messageId) {
                msg.copy(replyPreview = newContent.take(80))
            } else {
                msg
            }
        }

        currentEntry = currentEntry.copy(messages = updatedMessages)
        saveCurrentEntry()
    }

    fun deleteMessage(messageId: String) {
        // Find message
        val message = currentEntry.messages.find { it.id == messageId } ?: return

        // Only allow deleting messages from today
        if (!isMessageFromToday(message)) return

        // Remove the message
        val filteredMessages = currentEntry.messages.filterNot { it.id == messageId }

        // If deleted message had image, try delete file
        message.imageUri?.let { imagePath ->
            try {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    imageFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Also clear reply references from messages that replied to this deleted message
        val cleanedMessages = filteredMessages.map { msg ->
            if (msg.replyToMessageId == messageId) {
                msg.copy(replyToMessageId = null, replyPreview = null)
            } else {
                msg
            }
        }

        currentEntry = currentEntry.copy(messages = cleanedMessages)
        saveCurrentEntry()
    }

    // Helper to get a message by id from currentEntry (null safe)
    fun getMessageById(id: String?): ChatMessage? {
        if (id == null) return null
        return currentEntry.messages.find { it.id == id }
    }

    // FIXED: Helper function compatible with API 26+
    private fun isMessageFromToday(message: ChatMessage): Boolean {
        val messageDate = java.time.Instant.ofEpochMilli(message.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val entryDate = java.time.Instant.ofEpochMilli(currentEntry.dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return messageDate == entryDate
    }

    // FIXED: Check if message was added later (for UI display) - API 26 compatible
    fun isMessageAddedLater(message: ChatMessage): Boolean {
        val messageDate = java.time.Instant.ofEpochMilli(message.timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        val entryDate = java.time.Instant.ofEpochMilli(currentEntry.dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return messageDate != entryDate
    }

    // Support multiple mood selection with 1-3 limit
    fun toggleMood(mood: String) {
        // ✅ Allow editing mood for any entry
        // if (!canEditMood) return

        val moodTag = "#mood-$mood"
        val currentMoodTags = currentEntry.tags.filter { it.startsWith("#mood-") }

        val updatedTags = if (moodTag in currentEntry.tags) {
            // Deselect mood - remove it
            currentEntry.tags.filterNot { it == moodTag }
        } else {
            // Select mood - add it (if under limit)
            if (currentMoodTags.size >= 3) {
                // Already at limit, don't add
                return
            }
            currentEntry.tags + moodTag
        }

        // Update mood field to reflect selected moods
        val selectedMoods = updatedTags.filter { it.startsWith("#mood-") }
            .map { it.removePrefix("#mood-") }
        val moodString = if (selectedMoods.isNotEmpty()) selectedMoods.joinToString(",") else null

        currentEntry = currentEntry.copy(tags = updatedTags, mood = moodString)
        saveCurrentEntry()
    }

    // Get currently selected moods
    fun getSelectedMoods(): Set<String> {
        return currentEntry.tags
            .filter { it.startsWith("#mood-") }
            .map { it.removePrefix("#mood-") }
            .toSet()
    }

    private fun saveCurrentEntry() {
        viewModelScope.launch {
            try {
                // always sort by timestamp before saving (keeps deterministic order)
                currentEntry = currentEntry.copy(
                    messages = currentEntry.messages.sortedBy { it.timestamp }
                )

                repo.saveEntry(currentEntry)          // local Room
                // inside JournalViewModel.saveCurrentEntry() just before/after repo.saveEntry(currentEntry)
                Log.d("VM", "Saving entry messages: ${currentEntry.messages.joinToString { "${it.id}:${it.replyToMessageId}:${it.replyPreview}" }}")

            } catch (e: Exception) {
                e.printStackTrace()
                // optionally: show snackbar on network error
            }
        }
    }
}
