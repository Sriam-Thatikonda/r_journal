package com.baverika.r_journal.utils

import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.model.MilestoneStats
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MilestoneCalculator {

    fun calculatePastYearStats(
        entries: List<JournalEntry>,
        tasks: List<Task>
    ): MilestoneStats {
        val now = System.currentTimeMillis()
        val oneYearAgo = now - (365L * 24L * 60L * 60L * 1000L)

        // 1. Filter entries from past 365 days
        val pastYearEntries = entries.filter { it.dateMillis in oneYearAgo..now }

        if (pastYearEntries.isEmpty()) {
            val pastYearTasks = tasks.count { it.isCompleted }
            return MilestoneStats(
                totalWordsWritten = 0,
                mostActiveDay = "No entries yet",
                totalTasksCompleted = pastYearTasks,
                topMood = "No mood logged",
                totalDaysJournaled = 0,
                totalDaysInPeriod = 365
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

        for (entry in pastYearEntries) {
            val hasMessages = entry.messages.isNotEmpty() || entry.imageUris.isNotEmpty()
            if (hasMessages) {
                val dateStr = Instant.ofEpochMilli(entry.dateMillis)
                    .atZone(ZoneId.systemDefault())
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
                .atZone(ZoneId.systemDefault())
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

        // 5. Total Tasks Completed in past year
        val completedTasksCount = tasks.count { task ->
            task.isCompleted
        }

        return MilestoneStats(
            totalWordsWritten = grandTotalWords,
            mostActiveDay = mostActiveDayStr,
            totalTasksCompleted = completedTasksCount,
            topMood = topMoodStr,
            totalDaysJournaled = uniqueJournaledDays.size,
            totalDaysInPeriod = 365,
            totalImagesAttached = grandTotalImages,
            totalAudioNotesRecorded = grandTotalAudioNotes
        )
    }
}
