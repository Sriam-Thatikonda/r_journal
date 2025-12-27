// app/src/main/java/com/baverika/r_journal/data/remote/dto/JournalEntryDto.kt
package com.baverika.r_journal.data.remote.dto

data class JournalEntryDto(
    val dateMillis: Long,
    val id: String,
    val messages: List<ChatMessageDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val mood: String? = null,
    val imageUris: List<String> = emptyList()
)
