// app/src/main/java/com/baverika/r_journal/data/local/database/Migrations.kt

package com.baverika.r_journal.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE journal_entries ADD COLUMN mood TEXT")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE quick_notes (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())
    }
}

// FIXED: Migration from v3 → v4
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create new table with correct schema
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS quick_notes_new (
                id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """.trimIndent())

        // 2. Copy existing data from old table (only non-null IDs)
        database.execSQL("""
            INSERT INTO quick_notes_new (id, title, content, timestamp)
            SELECT id, title, content, timestamp 
            FROM quick_notes
            WHERE id IS NOT NULL
        """.trimIndent())

        // 3. Drop old table
        database.execSQL("DROP TABLE IF EXISTS quick_notes")

        // 4. Rename new table
        database.execSQL("ALTER TABLE quick_notes_new RENAME TO quick_notes")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create new table (journal_entries_new) with the correct primary key
        database.execSQL("""
            CREATE TABLE journal_entries_new (
                dateMillis INTEGER PRIMARY KEY NOT NULL,
                id TEXT NOT NULL,
                messages TEXT NOT NULL,
                tags TEXT NOT NULL,
                mood TEXT,
                imageUris TEXT NOT NULL
            )
        """)

        // 2. Copy ALL data from the old table to the new table
        database.execSQL("""
            INSERT INTO journal_entries_new (dateMillis, id, messages, tags, mood, imageUris)
            SELECT dateMillis, id, messages, tags, mood, imageUris
            FROM journal_entries
        """)

        // 3. Drop the old table
        database.execSQL("DROP TABLE journal_entries")

        // 4. Rename the new table
        database.execSQL("ALTER TABLE journal_entries_new RENAME TO journal_entries")
    }
}