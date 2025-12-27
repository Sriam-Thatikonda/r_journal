package com.baverika.r_journal.utils

import android.content.Context
import android.util.Log
import com.baverika.r_journal.data.local.database.JournalDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DbRestoreUtils {

    fun getBackups(context: Context): List<File> {
        val backupDir = File(context.getExternalFilesDir(null), "Backups")
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles { file ->
            file.name.startsWith("journal_backup_") && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun restoreBackup(context: Context, backupFile: File): Boolean {
        return try {
            val dbName = "journal_db"
            val dbPath = context.getDatabasePath(dbName)

            // Close the database if possible (Room doesn't expose close easily on singleton, 
            // but we can try to ensure no active queries by doing this on a fresh screen)
            // Ideally, we should close the RoomDatabase instance.
            // Since we can't easily access the singleton instance to close it here without 
            // exposing it, we will rely on file system overwrite and app restart.
            
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
