package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "craving_logs")
data class CravingLogEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val food: String,
    val location: String, // "Indoor" or "Outdoor"
    val quest: String,    // The generated quest(s) text
    val difficulty: String, // "Easy", "Medium", "Hard", "Boss"
    val createdAt: Long = System.currentTimeMillis(),
    
    val questCompleted: Boolean = false,
    val questCompletedAt: Long? = null,
    
    val foodEaten: Boolean = false,
    val foodEatenAt: Long? = null
)
