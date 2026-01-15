package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "life_tracker_entries",
    foreignKeys = [
        ForeignKey(
            entity = LifeTracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerId"])]
)
data class LifeTrackerEntry(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val trackerId: String,
    val dateMillis: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)
