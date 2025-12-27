// app/src/main/java/com/baverika/r_journal/data/local/entity/JournalEntry.kt

package com.baverika.r_journal.data.local.entity

import android.os.Build
import androidx.annotation.RequiresApi
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
    val localDate: LocalDate
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        get() = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(dateMillis), ZoneId.systemDefault())

    companion object {
        fun createForToday(): JournalEntry {
            val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            return JournalEntry(dateMillis = startOfDay)
        }
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        fun isToday(dateMillis: Long): Boolean {
            val entryDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(dateMillis), ZoneId.systemDefault())
            return entryDate == LocalDate.now()
        }
    }



    // ✅ No-arg constructor with explicit defaults
    constructor() : this(
        id = java.util.UUID.randomUUID().toString(),
        dateMillis = 0L,
        messages = emptyList(),
        tags = emptyList(),
        mood = null
    )
}