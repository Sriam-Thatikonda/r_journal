package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "tracker_history",
    foreignKeys = [
        ForeignKey(
            entity = Tracker::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["trackerId"])]
)
data class TrackerHistory(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val trackerId: String,
    val date: String, // format YYYY-MM-DD
    val value: Int
)
