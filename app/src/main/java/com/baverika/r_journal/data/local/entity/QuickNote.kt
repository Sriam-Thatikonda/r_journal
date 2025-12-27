// app/src/main/java/com/baverika/r_journal/data/local/entity/QuickNote.kt

package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_notes")
data class QuickNote(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val color: Long = 0xFF000000 // Default black background
) {
    constructor() : this(
        id = java.util.UUID.randomUUID().toString(),
        title = "",
        content = "",
        timestamp = System.currentTimeMillis(),
        color = 0xFF000000
    )
}