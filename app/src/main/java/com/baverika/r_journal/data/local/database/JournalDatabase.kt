package com.baverika.r_journal.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.baverika.r_journal.data.local.converters.Converters
import com.baverika.r_journal.data.local.dao.*
import com.baverika.r_journal.data.local.entity.*
import com.baverika.r_journal.quotes.data.QuoteDao
import com.baverika.r_journal.quotes.data.QuoteEntity

@Database(
    entities = [
        JournalEntry::class,
        QuickNote::class,
        Event::class,
        Habit::class,
        HabitLog::class,
        Password::class,
        QuoteEntity::class,
        Task::class,
        TaskCategory::class,
        LifeTracker::class,
        LifeTrackerEntry::class,
        CravingLogEntity::class
    ],
    version = 14,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JournalDatabase : RoomDatabase() {

    abstract fun journalDao(): JournalDao
    abstract fun quickNoteDao(): QuickNoteDao
    abstract fun eventDao(): EventDao
    abstract fun habitDao(): HabitDao
    abstract fun passwordDao(): PasswordDao
    abstract fun quoteDao(): QuoteDao
    abstract fun taskDao(): TaskDao
    abstract fun lifeTrackerDao(): LifeTrackerDao
    abstract fun cravingLogDao(): CravingLogDao

    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE journal_entries ADD COLUMN mood TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE quick_notes (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS quick_notes_new (
                        id TEXT PRIMARY KEY NOT NULL,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO quick_notes_new (id, title, content, timestamp)
                    SELECT id, title, content, timestamp 
                    FROM quick_notes
                    WHERE id IS NOT NULL
                """.trimIndent())
                db.execSQL("DROP TABLE IF EXISTS quick_notes")
                db.execSQL("ALTER TABLE quick_notes_new RENAME TO quick_notes")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE journal_entries_new (
                        dateMillis INTEGER PRIMARY KEY NOT NULL,
                        id TEXT NOT NULL,
                        messages TEXT NOT NULL,
                        tags TEXT NOT NULL,
                        mood TEXT,
                        imageUris TEXT NOT NULL
                    )
                """)
                db.execSQL("""
                    INSERT INTO journal_entries_new (dateMillis, id, messages, tags, mood, imageUris)
                    SELECT dateMillis, id, messages, tags, mood, imageUris
                    FROM journal_entries
                """)
                db.execSQL("DROP TABLE journal_entries")
                db.execSQL("ALTER TABLE journal_entries_new RENAME TO journal_entries")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `events` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `day` INTEGER NOT NULL, 
                        `month` INTEGER NOT NULL, 
                        `year` INTEGER, 
                        `type` TEXT NOT NULL, 
                        `isRecurring` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` TEXT NOT NULL, 
                        `title` TEXT NOT NULL, 
                        `description` TEXT NOT NULL, 
                        `frequency` TEXT NOT NULL, 
                        `color` INTEGER NOT NULL, 
                        `icon` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `isArchived` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `habit_logs` (
                        `id` TEXT NOT NULL, 
                        `habitId` TEXT NOT NULL, 
                        `dateMillis` INTEGER NOT NULL, 
                        `isCompleted` INTEGER NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_dateMillis` ON `habit_logs` (`habitId`, `dateMillis`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE quick_notes ADD COLUMN color INTEGER NOT NULL DEFAULT 4294967295")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `passwords` (
                        `id` TEXT NOT NULL, 
                        `siteName` TEXT NOT NULL, 
                        `username` TEXT NOT NULL, 
                        `passwordValue` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `updatedAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `quotes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `text` TEXT NOT NULL,
                        `author` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `task_categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `icon` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tasks` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `dueDate` INTEGER,
                        `priority` TEXT NOT NULL,
                        `categoryId` TEXT,
                        `isCompleted` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `reminderTime` INTEGER,
                        `isRecurring` INTEGER NOT NULL,
                        `recurringPattern` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`categoryId`) REFERENCES `task_categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `life_trackers` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `life_tracker_entries` (
                        `id` TEXT NOT NULL,
                        `trackerId` TEXT NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `note` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`trackerId`) REFERENCES `life_trackers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_life_tracker_entries_trackerId` ON `life_tracker_entries` (`trackerId`)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passwords ADD COLUMN type TEXT NOT NULL DEFAULT 'PASSWORD'")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `craving_logs` (
                        `id` TEXT NOT NULL,
                        `food` TEXT NOT NULL,
                        `location` TEXT NOT NULL,
                        `quest` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `questCompleted` INTEGER NOT NULL,
                        `questCompletedAt` INTEGER,
                        `foodEaten` INTEGER NOT NULL,
                        `foodEatenAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): JournalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "journal_db"
                ).addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                    MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                    MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                    MIGRATION_13_14
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}