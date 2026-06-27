// app/src/main/java/com/baverika/r_journal/data/local/entity/JournalEntry.kt

package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.ZoneId

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val dateMillis: Long = 0L, // ✅ NEW PRIMARY KEY: Stable and unique per day
    val id: String = java.util.UUID.randomUUID().toString(), // Keep UUID for external file reference
    val messages: List<ChatMessage> = emptyList(),
    val tags: List<String> = emptyList(),
    val mood: String? = null,
    val imageUris: List<String> = emptyList()
) {
    // API-26 safe: uses atZone() instead of LocalDate.ofInstant() (API 34+)
    val localDate: LocalDate
        get() = java.time.Instant.ofEpochMilli(dateMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

    companion object {
        fun createForToday(): JournalEntry {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            return JournalEntry(dateMillis = startOfDay)
        }
        fun isToday(dateMillis: Long): Boolean {
            val entryDate = java.time.Instant.ofEpochMilli(dateMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            return entryDate == LocalDate.now()
        }
    }



    // ✅ No-arg constructor with explicit defaults (imageUris included to avoid null)
    constructor() : this(
        id = java.util.UUID.randomUUID().toString(),
        dateMillis = 0L,
        messages = emptyList(),
        tags = emptyList(),
        mood = null,
        imageUris = emptyList()
    )
}