package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

data class PasswordHistoryItem(
    val passwordValue: String,
    val changedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "passwords")
data class Password(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val siteName: String,
    val username: String,
    val passwordValue: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val type: PasswordType = PasswordType.PASSWORD,
    val history: List<PasswordHistoryItem> = emptyList()
)


enum class PasswordType {
    PASSWORD,
    PIN;

    companion object {
        fun fromString(value: String): PasswordType {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                PASSWORD
            }
        }
    }
}
