package com.baverika.r_journal.data.local.entity

import java.time.LocalDate
import java.time.ZoneId

data class JournalEntrySummary(
    val id: String,
    val dateMillis: Long,
    val mood: String?,
    val tags: List<String>,
    val messageCount: Int,
    val imageCount: Int,
    val previewText: String?,
    val hasImages: Boolean,
    // ✅ Pre-calculated UI fields
    val moodEmojis: List<String>,
    val dayOfWeek: String,
    val dateFormatted: String,
    val searchableText: String = ""
) {
    val localDate: LocalDate
        get() = java.time.Instant.ofEpochMilli(dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
}
