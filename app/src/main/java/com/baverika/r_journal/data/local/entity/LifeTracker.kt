package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "life_trackers")
data class LifeTracker(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String, // Emoji or icon identifier
    val color: Long,  // ARGB color
    val createdAt: Long = System.currentTimeMillis()
)
