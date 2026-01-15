// app/src/main/java/com/baverika/r_journal/data/local/entity/ChatMessage.kt

package com.baverika.r_journal.data.local.entity

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: String = "user",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val voiceNoteUri: String? = null, // Path to voice note file
    val voiceNoteDuration: Long = 0L, // Duration in milliseconds
    val replyToMessageId: String? = null,
    val replyPreview: String? = null
) {
    constructor() : this(
        id = java.util.UUID.randomUUID().toString(),
        role = "user",
        content = "",
        timestamp = System.currentTimeMillis(),
        imageUri = null,
        voiceNoteUri = null,
        voiceNoteDuration = 0L,
        replyToMessageId = null,
        replyPreview = null
    )
}