package com.baverika.r_journal.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.baverika.r_journal.data.local.dao.JournalDao
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.remote.RetrofitClient
import com.baverika.r_journal.data.remote.toDto
import com.baverika.r_journal.data.remote.toEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

import com.baverika.r_journal.data.local.entity.JournalEntrySummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class JournalRepository(
    private val journalDao: JournalDao
) {
    val allEntries: Flow<List<JournalEntry>> = journalDao.getAllEntries()

    // ✅ NEW: Lightweight flow for the list view
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    val allEntrySummaries: Flow<List<JournalEntrySummary>> = allEntries
        .map { entries ->
            entries.map { entry ->
                JournalEntrySummary(
                    id = entry.id,
                    dateMillis = entry.dateMillis,
                    mood = entry.mood,
                    tags = entry.tags,
                    messageCount = entry.messages.size,
                    imageCount = entry.messages.count { it.imageUri != null },
                    previewText = entry.messages.firstOrNull()?.content?.take(80),
                    hasImages = entry.messages.any { it.imageUri != null },
                    // ✅ Pre-calculate UI strings here (Background Thread)
                    moodEmojis = entry.tags
                        .filter { it.startsWith("#mood-") }
                        .map { tag ->
                            when (tag.removePrefix("#mood-")) {
                                "happy" -> "\uD83D\uDE0A"
                                "calm" -> "\uD83D\uDE0C"
                                "anxious" -> "\uD83D\uDE30"
                                "sad" -> "\uD83D\uDE22"
                                "tired" -> "\uD83D\uDE34"
                                "excited" -> "\uD83E\uDD29"
                                else -> "😶"
                            }
                        },
                    dayOfWeek = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(entry.dateMillis), ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("EEE")),
                    dateFormatted = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(entry.dateMillis), ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
                )
            }
        }
        .flowOn(Dispatchers.IO) // Do the heavy mapping on background thread

    // ✅ NEW: Mood Stats Calculation
    data class MoodStats(
        val weeklyAverage: Float,
        val monthlyAverage: Float
    )

    val moodStats: Flow<MoodStats> = allEntries.map { entries ->
        val now = LocalDate.now()
        val zoneId = ZoneId.systemDefault()

        // Helper to get score from mood tag
        fun getMoodScore(tag: String): Int {
            return when (tag.removePrefix("#mood-")) {
                "happy", "excited" -> 5
                "calm" -> 4
                "tired" -> 3
                "anxious" -> 2
                "sad" -> 1
                else -> 0
            }
        }

        // Helper to calculate average for a list of entries
        fun calculateAverage(filteredEntries: List<JournalEntry>): Float {
            if (filteredEntries.isEmpty()) return 0f

            val dailyScores = filteredEntries.mapNotNull { entry ->
                val moodTags = entry.tags.filter { it.startsWith("#mood-") }
                if (moodTags.isEmpty()) return@mapNotNull null

                // Average of moods for this day (Max 3)
                val sum = moodTags.sumOf { getMoodScore(it) }
                sum.toFloat() / moodTags.size
            }

            if (dailyScores.isEmpty()) return 0f
            return dailyScores.average().toFloat()
        }

        val weeklyEntries = entries.filter {
            val date = java.time.Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate()
            date.isAfter(now.minusDays(7))
        }

        val monthlyEntries = entries.filter {
            val date = java.time.Instant.ofEpochMilli(it.dateMillis).atZone(zoneId).toLocalDate()
            date.isAfter(now.minusDays(30))
        }

        MoodStats(
            weeklyAverage = calculateAverage(weeklyEntries),
            monthlyAverage = calculateAverage(monthlyEntries)
        )
    }.flowOn(Dispatchers.IO)

    private fun todayStartMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

    suspend fun getOrCreateTodaysEntry(): JournalEntry {
        val todayStart = todayStartMillis()
        return journalDao.getEntryByDate(todayStart) ?: JournalEntry.createForToday()
    }

    suspend fun saveEntry(entry: JournalEntry) {
        // always keep messages ordered
        val sorted = entry.copy(
            messages = entry.messages.sortedBy { it.timestamp }
        )
        journalDao.insertEntry(sorted)
    }

    suspend fun getEntryById(id: String): JournalEntry? {
        return journalDao.getEntryById(id)
    }

    fun search(query: String): Flow<List<JournalEntry>> = journalDao.searchEntries(query)

    suspend fun upsertEntry(entry: JournalEntry) {
        saveEntry(entry)
    }

    suspend fun getOrCreateEntryForDate(dateMillis: Long): JournalEntry {
        return journalDao.getEntryByDate(dateMillis) ?: run {
            val newEntry = JournalEntry(dateMillis = dateMillis)
            journalDao.insertEntry(newEntry)
            newEntry
        }
    }

    // ---------- NEW: sync with Flask API ----------

    suspend fun syncTodayFromServer(local: JournalEntry?): JournalEntry? {
        return withContext(Dispatchers.IO) {
            try {
                val dto = RetrofitClient.getApi().getToday()
                val remote = dto.toEntity()

                // Base = what we already have locally
                val base = local ?: remote

                // Merge messages: local(app) + remote(site)
                // Deduplicate by ID, keeping the one with the latest timestamp (or prefer remote if equal/unknown)
                val allMessages = (local?.messages ?: emptyList()) + remote.messages
                val distinctMessages = allMessages
                    .groupBy { it.id }
                    .map { (_, duplicates) ->
                        // Pick the version with the highest timestamp (latest edit)
                        duplicates.maxByOrNull { it.timestamp }!!
                    }
                    .sortedBy { it.timestamp }

                // ✅ Only update messages (and optionally id).
                //    Mood, tags, imageUris stay exactly as in 'base' (app).
                val mergedEntry = base.copy(
                    id = remote.id,          // or keep base.id if you prefer
                    messages = distinctMessages
                    // tags = base.tags
                    // mood = base.mood
                    // imageUris = base.imageUris
                )

                saveEntry(mergedEntry)
                mergedEntry
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


//    suspend fun pushTodayToServer(entry: JournalEntry) {
//        try {
//            val dto = entry.toDto()
//            Log.d("JournalRepo", "Pushing to server: ${dto.messages.size} messages")
//            val savedDto = RetrofitClient.api.saveToday(dto)
//            val savedEntity = savedDto.toEntity()
//            saveEntry(savedEntity) // keep DB in sync with whatever server has
//        } catch (e: Exception) {
//            e.printStackTrace()
//            Log.e("JournalRepo", "pushTodayToServer failed", e)
//            // You can decide to ignore or bubble up error
//        }
//    }

    // ---------- Habits ----------
    private val habitDao = com.baverika.r_journal.data.local.database.JournalDatabase.getDatabase(com.baverika.r_journal.RJournalApp.instance).habitDao()

    val allActiveHabits: Flow<List<com.baverika.r_journal.data.local.entity.Habit>> = habitDao.getAllActiveHabits()

    suspend fun getHabitById(id: String): com.baverika.r_journal.data.local.entity.Habit? {
        return habitDao.getHabitById(id)
    }

    fun getHabitLogsBetween(startMillis: Long, endMillis: Long): Flow<List<com.baverika.r_journal.data.local.entity.HabitLog>> {
        return habitDao.getHabitLogsBetween(startMillis, endMillis)
    }

    suspend fun addHabit(habit: com.baverika.r_journal.data.local.entity.Habit) {
        habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: com.baverika.r_journal.data.local.entity.Habit) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habit: com.baverika.r_journal.data.local.entity.Habit) {
        habitDao.deleteHabit(habit)
    }

    fun getHabitLogsForDate(dateMillis: Long): Flow<List<com.baverika.r_journal.data.local.entity.HabitLog>> {
        return habitDao.getHabitLogsForDate(dateMillis)
    }

    suspend fun toggleHabitCompletion(habitId: String, dateMillis: Long, isCompleted: Boolean) {
        if (isCompleted) {
            val log = com.baverika.r_journal.data.local.entity.HabitLog(
                habitId = habitId,
                dateMillis = dateMillis,
                isCompleted = true
            )
            habitDao.insertHabitLog(log)
        } else {
            habitDao.deleteHabitLog(habitId, dateMillis)
        }
    }

    // Synchronous methods for widget (runs on background thread)
    fun getAllActiveHabits(): List<com.baverika.r_journal.data.local.entity.Habit> {
        return habitDao.getAllActiveHabitsSync()
    }

    fun getHabitLogsForDateSync(dateMillis: Long): List<com.baverika.r_journal.data.local.entity.HabitLog> {
        return habitDao.getHabitLogsForDateSync(dateMillis)
    }
}
