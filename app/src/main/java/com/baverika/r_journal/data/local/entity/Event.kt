package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "events")
data class Event(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val day: Int,
    val month: Int, // 1-12
    val year: Int? = null, // Optional year for calculating age/years
    val type: EventType = EventType.CUSTOM,
    val isRecurring: Boolean = true
)

enum class EventType {
    BIRTHDAY,
    ANNIVERSARY,
    MEETING,
    CUSTOM
}
