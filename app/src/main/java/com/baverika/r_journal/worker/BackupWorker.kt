package com.baverika.r_journal.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dbName = "journal_database"
            val dbPath = applicationContext.getDatabasePath(dbName)

            if (!dbPath.exists()) {
                Log.e("BackupWorker", "Database file not found: ${dbPath.absolutePath}")
                return Result.failure()
            }

            // Create Backups directory in app-specific external storage
            // /Android/data/com.baverika.r_journal/files/Backups
            val backupDir = File(applicationContext.getExternalFilesDir(null), "Backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            // Create backup file name with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "journal_backup_$timestamp.db")

            // Copy file
            dbPath.copyTo(backupFile, overwrite = true)
            Log.d("BackupWorker", "Backup created: ${backupFile.absolutePath}")

            // Retention Policy: Keep only last 2 backups
            cleanOldBackups(backupDir)

            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup failed", e)
            Result.failure()
        }
    }

    private fun cleanOldBackups(backupDir: File) {
        val files = backupDir.listFiles { file ->
            file.name.startsWith("journal_backup_") && file.name.endsWith(".db")
        } ?: return

        // Sort by modification time (oldest first)
        val sortedFiles = files.sortedBy { it.lastModified() }

        // If more than 2, delete the oldest ones
        if (sortedFiles.size > 2) {
            val filesToDelete = sortedFiles.take(sortedFiles.size - 2)
            filesToDelete.forEach { file ->
                if (file.delete()) {
                    Log.d("BackupWorker", "Deleted old backup: ${file.name}")
                } else {
                    Log.w("BackupWorker", "Failed to delete old backup: ${file.name}")
                }
            }
        }
    }
}
