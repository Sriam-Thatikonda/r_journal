package com.baverika.r_journal.quotes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for storing motivational quotes.
 * Uses soft delete pattern with isActive flag.
 */
@Entity(tableName = "quotes")
data class QuoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val text: String,
    val author: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) {
    // Secondary constructor for JSON import compatibility
    constructor() : this(
        id = 0,
        text = "",
        author = null,
        createdAt = System.currentTimeMillis(),
        isActive = true
    )
}
