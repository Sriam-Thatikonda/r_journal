// app/src/main/java/com/baverika/r_journal/data/remote/dto/ChatMessageDto.kt
package com.baverika.r_journal.data.remote.dto

data class ChatMessageDto(
    val id: String? = null,
    val role: String? = "user",
    val text: String? = "",
    val timestamp: Long? = null,
    val imageUri: String? = null
)
