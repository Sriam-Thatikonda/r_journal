package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "trackers")
data class Tracker(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val emoji: String,
    val color: Long,
    val goal: Int,
    val currentCount: Int = 0,
    val incrementStep: Int = 1,
    val resetFrequency: String = "Never", // "Daily", "Weekly", "Never"
    val createdDate: Long = System.currentTimeMillis(),
    val updatedDate: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val trackerType: String = "COUNTER", // Future-proofing: "COUNTER", "STREAK", "TIMER", etc.
    val notes: String? = null
)
