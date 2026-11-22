package com.baverika.r_journal.utils

import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.model.MilestoneStats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MilestoneCalculator {

    /**
     * Calculates statistics for the calendar year from January 1st to December 31st.
     * Defaults to the current calendar year.
     */
    fun calculateCurrentYearStats(
        entries: List<JournalEntry>,
        tasks: List<Task>,
        targetYear: Int = LocalDate.now().year
    ): MilestoneStats {
        val zoneId = ZoneId.systemDefault()
        val startOfYearDate = LocalDate.of(targetYear, 1, 1)
        val daysInYear = startOfYearDate.lengthOfYear()

        // 1. Filter entries strictly between Jan 1st and Dec 31st of the target year
        val currentYearEntries = entries.filter { entry ->
            val entryDate = Instant.ofEpochMilli(entry.dateMillis)
                .atZone(zoneId)
                .toLocalDate()
            entryDate.year == targetYear
        }

        // Filter completed tasks belonging to the target year (by dueDate, updatedAt, or createdAt)
        val completedTasksCount = tasks.count { task ->
            if (!task.isCompleted) return@count false
            val dueYear = task.dueDate?.let { Instant.ofEpochMilli(it).atZone(zoneId).toLocalDate().year }
            val updatedYear = if (task.updatedAt > 0L) Instant.ofEpochMilli(task.updatedAt).atZone(zoneId).toLocalDate().year else null
            val createdYear = Instant.ofEpochMilli(task.createdAt).atZone(zoneId).toLocalDate().year
            dueYear == targetYear || updatedYear == targetYear || (dueYear == null && updatedYear == null && createdYear == targetYear)
        }

        if (currentYearEntries.isEmpty()) {
            return MilestoneStats(
                year = targetYear,
                totalWordsWritten = 0,
                mostActiveDay = "No entries yet",
                totalTasksCompleted = completedTasksCount,
                topMood = "No mood logged",
                totalDaysJournaled = 0,
                totalDaysInPeriod = daysInYear
            )
        }

        // 2. Total Words, Images, Voice Notes & Most Active Day
        var grandTotalWords = 0
        var grandTotalImages = 0
        var grandTotalAudioNotes = 0
        var peakWordCount = 0
        var peakEntryDateMillis = 0L

        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

        val uniqueJournaledDays = mutableSetOf<String>()
        val moodCounts = mutableMapOf<String, Int>()

        for (entry in currentYearEntries) {
            val hasMessages = entry.messages.isNotEmpty() || entry.imageUris.isNotEmpty()
            if (hasMessages) {
                val dateStr = Instant.ofEpochMilli(entry.dateMillis)
                    .atZone(zoneId)
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                uniqueJournaledDays.add(dateStr)
            }

            // Entry-level images
            grandTotalImages += entry.imageUris.size

            var entryWords = 0
            for (msg in entry.messages) {
                if (msg.content.isNotBlank()) {
                    val words = msg.content.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
                    entryWords += words
                }
                if (!msg.imageUri.isNullOrBlank()) {
                    grandTotalImages++
                }
                if (!msg.voiceNoteUri.isNullOrBlank()) {
                    grandTotalAudioNotes++
                }
            }
            grandTotalWords += entryWords

            if (entryWords > peakWordCount) {
                peakWordCount = entryWords
                peakEntryDateMillis = entry.dateMillis
            }

            // Track mood frequency
            entry.mood?.let { m ->
                if (m.isNotBlank()) {
                    moodCounts[m] = moodCounts.getOrDefault(m, 0) + 1
                }
            }
            for (tag in entry.tags) {
                if (tag.startsWith("#mood-")) {
                    val moodName = tag.removePrefix("#mood-")
                    moodCounts[moodName] = moodCounts.getOrDefault(moodName, 0) + 1
                }
            }
        }

        // 3. Most Active Day Formatting
        val mostActiveDayStr = if (peakWordCount > 0 && peakEntryDateMillis > 0) {
            val formattedDate = Instant.ofEpochMilli(peakEntryDateMillis)
                .atZone(zoneId)
                .format(dateFormatter)
            "$formattedDate ($peakWordCount words)"
        } else {
            "No active writing day"
        }

        // 4. Top Mood
        val topMoodStr = if (moodCounts.isNotEmpty()) {
            val (bestMood, count) = moodCounts.maxByOrNull { it.value }!!
            val totalMoodLogs = moodCounts.values.sum()
            val pct = if (totalMoodLogs > 0) (count * 100) / totalMoodLogs else 0
            val capitalizedMood = bestMood.replaceFirstChar { it.uppercase() }
            "$capitalizedMood ($pct% of logs)"
        } else {
            "No mood logged"
        }

        return MilestoneStats(
            year = targetYear,
            totalWordsWritten = grandTotalWords,
            mostActiveDay = mostActiveDayStr,
            totalTasksCompleted = completedTasksCount,
            topMood = topMoodStr,
            totalDaysJournaled = uniqueJournaledDays.size,
            totalDaysInPeriod = daysInYear,
            totalImagesAttached = grandTotalImages,
            totalAudioNotesRecorded = grandTotalAudioNotes
        )
    }

    /**
     * Backward-compatible alias for calculateCurrentYearStats.
     */
    fun calculatePastYearStats(
        entries: List<JournalEntry>,
        tasks: List<Task>
    ): MilestoneStats = calculateCurrentYearStats(entries, tasks)
}
