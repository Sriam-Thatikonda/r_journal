package com.baverika.r_journal.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.baverika.r_journal.data.local.entity.Password
import com.baverika.r_journal.repository.PasswordRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PasswordExportUtils {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Export passwords to a JSON file in Downloads folder.
     * Returns (success, message/path)
     */
    suspend fun exportPasswords(
        context: Context,
        passwords: List<Password>
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            if (passwords.isEmpty()) {
                return@withContext Pair(false, "No passwords to export")
            }

            val exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
            val fileName = "r_journal_passwords_$timestamp.json"
            val exportFile = File(exportDir, fileName)

            // Convert to exportable format (without internal IDs being critical)
            val exportData = PasswordExportData(
                version = 1,
                exportedAt = System.currentTimeMillis(),
                count = passwords.size,
                passwords = passwords.map { password ->
                    ExportablePassword(
                        siteName = password.siteName,
                        username = password.username,
                        passwordValue = password.passwordValue,
                        createdAt = password.createdAt,
                        updatedAt = password.updatedAt
                    )
                }
            )

            val json = gson.toJson(exportData)
            exportFile.writeText(json)

            Pair(true, "Exported ${passwords.size} passwords to:\n${exportFile.absolutePath}")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Export failed: ${e.message}")
        }
    }

    /**
     * Import passwords from a JSON file.
     * Returns (success, message)
     */
    suspend fun importPasswords(
        context: Context,
        uri: Uri,
        repository: PasswordRepository
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Pair(false, "Could not open file")

            val json = inputStream.bufferedReader().use { it.readText() }
            
            val exportData = gson.fromJson(json, PasswordExportData::class.java)
                ?: return@withContext Pair(false, "Invalid password backup file format")

            var importedCount = 0
            
            exportData.passwords.forEach { exportable ->
                val password = Password(
                    siteName = exportable.siteName,
                    username = exportable.username,
                    passwordValue = exportable.passwordValue,
                    createdAt = exportable.createdAt,
                    updatedAt = exportable.updatedAt
                )
                repository.insertPassword(password)
                importedCount++
            }

            Pair(true, "Successfully imported $importedCount passwords")
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(false, "Import failed: ${e.message}")
        }
    }
}

// Data classes for JSON serialization
data class PasswordExportData(
    val version: Int,
    val exportedAt: Long,
    val count: Int,
    val passwords: List<ExportablePassword>
)

data class ExportablePassword(
    val siteName: String,
    val username: String,
    val passwordValue: String,
    val createdAt: Long,
    val updatedAt: Long
)
