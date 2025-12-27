package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId", "dateMillis"], unique = true)]
)
data class HabitLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val habitId: String,
    val dateMillis: Long, // Start of day timestamp
    val isCompleted: Boolean = true,
    val timestamp: Long = System.currentTimeMillis() // When it was marked
)
