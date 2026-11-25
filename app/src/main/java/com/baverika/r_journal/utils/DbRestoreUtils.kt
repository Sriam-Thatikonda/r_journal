package com.baverika.r_journal.utils

import android.content.Context
import android.util.Log
import com.baverika.r_journal.data.local.database.JournalDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DbRestoreUtils {

    private val SQLITE_HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)

    fun getBackups(context: Context): List<File> {
        val internalDir = File(context.filesDir, "Backups")
        val legacyExternalDir = File(context.getExternalFilesDir(null), "Backups")

        val files = mutableListOf<File>()
        for (dir in listOf(internalDir, legacyExternalDir)) {
            if (dir.exists()) {
                dir.listFiles { file ->
                    file.isFile && file.name.startsWith("journal_backup_") && file.name.endsWith(".db")
                }?.let { files.addAll(it) }
            }
        }
        return files.sortedByDescending { it.lastModified() }
    }

    private fun isValidSqliteFile(file: File): Boolean {
        if (!file.exists() || !file.isFile || file.length() < SQLITE_HEADER.size) return false
        return try {
            val header = ByteArray(SQLITE_HEADER.size)
            FileInputStream(file).use { input ->
                val read = input.read(header)
                read == SQLITE_HEADER.size && header.contentEquals(SQLITE_HEADER)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun restoreBackup(context: Context, backupFile: File): Boolean {
        return try {
            if (!isValidSqliteFile(backupFile)) {
                Log.e("DbRestoreUtils", "Invalid SQLite backup file: ${backupFile.name}")
                return false
            }

            val dbName = "journal_db"
            val dbPath = context.getDatabasePath(dbName)

            // Close Room instance cleanly before replacing database files
            JournalDatabase.closeAndResetInstance()

            if (dbPath.exists()) {
                dbPath.delete()
            }

            // Also delete SHM and WAL files if they exist (for WAL mode)
            val shmFile = File(dbPath.parent, "$dbName-shm")
            if (shmFile.exists()) shmFile.delete()

            val walFile = File(dbPath.parent, "$dbName-wal")
            if (walFile.exists()) walFile.delete()

            // Copy backup to db path
            FileInputStream(backupFile).use { input ->
                FileOutputStream(dbPath).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("DbRestoreUtils", "Restored from ${backupFile.name}")
            true
        } catch (e: Exception) {
            Log.e("DbRestoreUtils", "Restore failed", e)
            false
        }
    }
}
