package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val frequency: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // Default: all days (1=Mon, 7=Sun)
    val color: Int = 0xFF6200EE.toInt(), // Default Purple
    val icon: String = "check", // Default icon name
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)
