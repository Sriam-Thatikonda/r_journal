package com.baverika.r_journal

import com.baverika.r_journal.data.local.entity.ChatMessage
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.utils.MilestoneCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class MilestoneCalculatorTest {

    private val zoneId = ZoneId.systemDefault()

    private fun dateToMillis(year: Int, month: Int, day: Int): Long {
        return LocalDate.of(year, month, day)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    @Test
    fun calculateCurrentYearStats_includesOnlyTargetYearJan1ToDec31() {
        val targetYear = 2026

        val entries = listOf(
            // Prior year (2025-12-31) - should be excluded
            JournalEntry(
                dateMillis = dateToMillis(2025, 12, 31),
                messages = listOf(ChatMessage(id = "1", content = "Old year note"))
            ),
            // Target year start (2026-01-01) - should be included
            JournalEntry(
                dateMillis = dateToMillis(2026, 1, 1),
                messages = listOf(
                    ChatMessage(id = "2", content = "New Year resolution"),
                    ChatMessage(id = "3", content = "Another thought")
                ),
                mood = "happy"
            ),
            // Target year mid (2026-06-15) - should be included
            JournalEntry(
                dateMillis = dateToMillis(2026, 6, 15),
                messages = listOf(ChatMessage(id = "4", content = "Mid year entry with five words")),
                mood = "happy",
                imageUris = listOf("file://image1.jpg")
            ),
            // Target year end (2026-12-31) - should be included
            JournalEntry(
                dateMillis = dateToMillis(2026, 12, 31),
                messages = listOf(ChatMessage(id = "5", content = "Last day of 2026")),
                mood = "calm"
            ),
            // Next year (2027-01-01) - should be excluded
            JournalEntry(
                dateMillis = dateToMillis(2027, 1, 1),
                messages = listOf(ChatMessage(id = "6", content = "Future entry"))
            )
        )

        val tasks = listOf(
            // Completed in 2026
            Task(
                id = "t1",
                title = "Task 1",
                isCompleted = true,
                dueDate = dateToMillis(2026, 3, 10)
            ),
            // Completed in 2025 (should be excluded)
            Task(
                id = "t2",
                title = "Task 2",
                isCompleted = true,
                dueDate = dateToMillis(2025, 12, 20),
                updatedAt = dateToMillis(2025, 12, 20),
                createdAt = dateToMillis(2025, 12, 20)
            ),
            // Incomplete in 2026 (should be excluded)
            Task(
                id = "t3",
                title = "Task 3",
                isCompleted = false,
                dueDate = dateToMillis(2026, 4, 1)
            )
        )

        val stats = MilestoneCalculator.calculateCurrentYearStats(entries, tasks, targetYear)

        assertEquals(targetYear, stats.year)
        assertEquals(365, stats.totalDaysInPeriod)
        assertEquals(3, stats.totalDaysJournaled) // Jan 1, Jun 15, Dec 31
        assertEquals(1, stats.totalTasksCompleted)
        assertEquals(1, stats.totalImagesAttached)
        assertEquals("Happy (66% of logs)", stats.topMood)
    }

    @Test
    fun calculateCurrentYearStats_emptyEntries() {
        val targetYear = 2026
        val stats = MilestoneCalculator.calculateCurrentYearStats(emptyList(), emptyList(), targetYear)

        assertEquals(targetYear, stats.year)
        assertEquals(0, stats.totalWordsWritten)
        assertEquals("No entries yet", stats.mostActiveDay)
        assertEquals("No mood logged", stats.topMood)
        assertEquals(0, stats.totalDaysJournaled)
        assertEquals(365, stats.totalDaysInPeriod)
        assertEquals(0, stats.totalTasksCompleted)
    }
}
