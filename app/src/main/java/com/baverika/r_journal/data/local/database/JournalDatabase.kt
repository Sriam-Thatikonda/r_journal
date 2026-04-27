package com.baverika.r_journal.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.baverika.r_journal.data.local.converters.Converters
import com.baverika.r_journal.data.local.dao.EventDao
import com.baverika.r_journal.data.local.dao.HabitDao
import com.baverika.r_journal.data.local.dao.JournalDao
import com.baverika.r_journal.data.local.dao.QuickNoteDao
import com.baverika.r_journal.data.local.dao.PasswordDao
import com.baverika.r_journal.data.local.dao.TaskDao
import com.baverika.r_journal.data.local.dao.LifeTrackerDao
import com.baverika.r_journal.quotes.data.QuoteDao
import com.baverika.r_journal.quotes.data.QuoteEntity

import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.Habit
import com.baverika.r_journal.data.local.entity.HabitLog
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.data.local.entity.Password
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskCategory
import com.baverika.r_journal.data.local.entity.LifeTracker
import com.baverika.r_journal.data.local.entity.LifeTrackerEntry


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
        LifeTrackerEntry::class
    ],
    version = 13,
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


    companion object {
        @Volatile
        private var INSTANCE: JournalDatabase? = null

        // Migrations 1-4 are imported from Migrations.kt
        // (MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
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
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create Habits table
                db.execSQL(
                    """
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
                    """.trimIndent()
                )
                // Create HabitLogs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_logs` (
                        `id` TEXT NOT NULL, 
                        `habitId` TEXT NOT NULL, 
                        `dateMillis` INTEGER NOT NULL, 
                        `isCompleted` INTEGER NOT NULL, 
                        `timestamp` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                // Create Index for HabitLogs
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_dateMillis` ON `habit_logs` (`habitId`, `dateMillis`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add color column to quick_notes table
                db.execSQL("ALTER TABLE quick_notes ADD COLUMN color INTEGER NOT NULL DEFAULT 4294967295")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `passwords` (
                        `id` TEXT NOT NULL, 
                        `siteName` TEXT NOT NULL, 
                        `username` TEXT NOT NULL, 
                        `passwordValue` TEXT NOT NULL, 
                        `createdAt` INTEGER NOT NULL, 
                        `updatedAt` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create quotes table for Motivational Quotes feature
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quotes` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `text` TEXT NOT NULL,
                        `author` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create task_categories table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_categories` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `color` INTEGER NOT NULL,
                        `icon` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                // Create tasks table
                db.execSQL(
                    """
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
                    """.trimIndent()
                )

                // Create index for categoryId
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_categoryId` ON `tasks` (`categoryId`)")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create life_trackers table
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

                // Create life_tracker_entries table
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
                
                // Create index for entries
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_life_tracker_entries_trackerId` ON `life_tracker_entries` (`trackerId`)")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add type column to passwords table, default to 'PASSWORD'
                db.execSQL("ALTER TABLE passwords ADD COLUMN type TEXT NOT NULL DEFAULT 'PASSWORD'")
            }
        }

        fun getDatabase(context: Context): JournalDatabase {

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JournalDatabase::class.java,
                    "journal_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)

                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}