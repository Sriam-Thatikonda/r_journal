// app/src/main/java/com/baverika/r_journal/data/remote/RemoteMappers.kt
package com.baverika.r_journal.data.remote

import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.remote.dto.ChatMessageDto
import com.baverika.r_journal.data.remote.dto.JournalEntryDto
import java.util.UUID

fun ChatMessageDto.toEntity(): ChatMessage {
    return ChatMessage(
        id = id ?: UUID.randomUUID().toString(),
        role = role ?: "user",
        content = text ?: "",                 // API "text" -> entity "content"
        timestamp = timestamp ?: System.currentTimeMillis(),
        imageUri = imageUri
    )
}

fun ChatMessage.toDto(): ChatMessageDto {
    return ChatMessageDto(
        id = id,
        role = role,
        text = content,                       // entity "content" -> API "text"
        timestamp = timestamp,
        imageUri = imageUri
    )
}

fun JournalEntryDto.toEntity(): JournalEntry {
    return JournalEntry(
        dateMillis = dateMillis,
        id = id,
        messages = messages.map { it.toEntity() }.sortedBy { it.timestamp },
        tags = tags,
        mood = mood,
        imageUris = imageUris
    )
}

fun JournalEntry.toDto(): JournalEntryDto {
    return JournalEntryDto(
        dateMillis = dateMillis,
        id = id,
        messages = messages.sortedBy { it.timestamp }.map { it.toDto() },
        tags = tags,
        mood = mood,
        imageUris = imageUris
    )
}
