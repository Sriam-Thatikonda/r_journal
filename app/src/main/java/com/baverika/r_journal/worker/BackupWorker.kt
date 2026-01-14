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
            val dbName = "journal_db"
            val dbFolder = applicationContext.getDatabasePath(dbName).parentFile
            
            // Source paths
            val dbFile = File(dbFolder, dbName)
            val walFile = File(dbFolder, "$dbName-wal")
            val shmFile = File(dbFolder, "$dbName-shm")

            if (!dbFile.exists()) {
                Log.e("BackupWorker", "Database file not found: ${dbFile.absolutePath}")
                return Result.failure()
            }

            // Target: App-specific external storage (Backups folder) using the same path as DbRestoreUtils
            val backupRoot = File(applicationContext.getExternalFilesDir(null), "Backups")
            if (!backupRoot.exists()) {
                backupRoot.mkdirs()
            }

            // Create timestamped filename
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val destDbFile = File(backupRoot, "journal_backup_$timestamp.db")
            
            // Copy DB file
            dbFile.copyTo(destDbFile, overwrite = true)
            
            // Copy WAL/SHM if they exist (Critical for data integrity in WAL mode)
            // Note: For a restore to work simply, typically we just need the main DB if it was checkpointed, 
            // but relying on that is risky.
            // DbRestoreUtils expects a single .db file. 
            // The Safest way for a single-file backup is to checkpoint properly or include -wal in the backup naming scheme 
            // or ZIP them.
            // However, DbRestoreUtils just copies the .db file back. If WAL is active, this might result in data loss 
            // of the latest transactions. 
            // To fix this properly for a single file restore: trigger a checkpoint before copy.
            checkpointWal(applicationContext, dbName)

            // Re-copy after checkpoint to ensure everything is in the main .db file
            dbFile.copyTo(destDbFile, overwrite = true)

            Log.d("BackupWorker", "Backup created at: ${destDbFile.absolutePath}")

            // Retention Policy: Keep only 5 most recent backup FILES
            cleanOldBackups(backupRoot)

            Result.success()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Backup failed", e)
            Result.failure()
        }
    }

    private fun checkpointWal(context: Context, dbName: String) {
        try {
            // Force a WAL checkpoint to move data to the main DB file
            val db = androidx.room.Room.databaseBuilder(
                context, 
                com.baverika.r_journal.data.local.database.JournalDatabase::class.java, 
                dbName
            ).build()
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").close()
            db.close()
        } catch (e: Exception) {
            Log.e("BackupWorker", "Checkpoint failed", e)
        }
    }

    private fun cleanOldBackups(backupRoot: File) {
        // List files that start with "journal_backup_" and end with ".db"
        val backupFiles = backupRoot.listFiles { file -> 
            file.isFile && file.name.startsWith("journal_backup_") && file.name.endsWith(".db")
        } ?: return

        // Sort by modification time (newest first for logic simplicity, or oldest first to delete)
        val sortedFiles = backupFiles.sortedBy { it.lastModified() }

        // Keep last 5 (delete if count > 5)
        if (sortedFiles.size > 5) {
            val filesToDelete = sortedFiles.take(sortedFiles.size - 5)
            filesToDelete.forEach { file ->
                file.delete() // Just delete the file
                Log.d("BackupWorker", "Deleted old backup file: ${file.name}")
            }
        }
    }
}
