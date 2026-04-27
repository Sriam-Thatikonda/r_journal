package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "passwords")
data class Password(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val siteName: String,
    val username: String,
    val passwordValue: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val type: PasswordType = PasswordType.PASSWORD
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
