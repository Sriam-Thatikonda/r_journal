package com.baverika.r_journal

import com.baverika.r_journal.data.local.dao.TrackerDao
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.data.local.entity.TrackerHistory
import com.baverika.r_journal.repository.TrackerRepository
import com.baverika.r_journal.utils.SecurityUtils
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class SecurityAndBugFixesTest {

    private class FakeTrackerDao(initialTrackers: List<Tracker>) : TrackerDao {
        val trackers = initialTrackers.associateBy { it.id }.toMutableMap()
        val histories = mutableListOf<TrackerHistory>()

        override fun getAllTrackersFlow(): Flow<List<Tracker>> = flowOf(trackers.values.filter { !it.archived })
        override fun getAllTrackers(): List<Tracker> = trackers.values.filter { !it.archived }
        override fun getAllTrackersIncludeArchived(): List<Tracker> = trackers.values.toList()
        override suspend fun getTrackerById(id: String): Tracker? = trackers[id]
        override fun getTrackerByIdFlow(id: String): Flow<Tracker?> = flowOf(trackers[id])
        override suspend fun insertTracker(tracker: Tracker) { trackers[tracker.id] = tracker }
        override suspend fun updateTracker(tracker: Tracker) { trackers[tracker.id] = tracker }
        override suspend fun deleteTracker(tracker: Tracker) { trackers.remove(tracker.id) }
        override suspend fun insertHistory(history: TrackerHistory) { histories.add(history) }
        override suspend fun updateHistory(history: TrackerHistory) {
            val idx = histories.indexOfFirst { it.id == history.id }
            if (idx >= 0) histories[idx] = history
        }
        override fun getHistoryForTracker(trackerId: String): Flow<List<TrackerHistory>> =
            flowOf(histories.filter { it.trackerId == trackerId })
        override suspend fun getAllHistory(): List<TrackerHistory> = histories.toList()
        override suspend fun getHistoryForTrackerAndDate(trackerId: String, date: String): TrackerHistory? =
            histories.firstOrNull { it.trackerId == trackerId && it.date == date }
    }

    @Test
    fun passphraseEncryption_roundTripsAndRejectsWrongPassphrase() {
        val secretJson = """[{"id":"1","siteName":"example.com","passwordValue":"s3cr3t!"}]"""
        val passphrase = "correct-horse-battery-staple"

        val encrypted = SecurityUtils.encryptWithPassphrase(secretJson, passphrase)
        assertNotEquals(secretJson, encrypted)
        assertFalse(encrypted.contains("s3cr3t!"))

        val decrypted = SecurityUtils.decryptWithPassphrase(encrypted, passphrase)
        assertEquals(secretJson, decrypted)

        try {
            SecurityUtils.decryptWithPassphrase(encrypted, "wrong-passphrase")
            fail("Expected SecurityException for wrong passphrase")
        } catch (_: SecurityException) {
            // Expected
        }

        try {
            SecurityUtils.decryptWithPassphrase("c2hvcnQ=", passphrase)
            fail("Expected SecurityException for short/invalid payload")
        } catch (_: SecurityException) {
            // Expected
        }
    }

    @Test
    fun trackerRepository_concurrentResetRunsOnceAndFlowIsReadOnly() = runTest {
        val twoDaysAgo = System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000
        val staleTracker = Tracker(
            id = "water",
            title = "Water",
            emoji = "💧",
            color = 0xFF2196F3L,
            currentCount = 5,
            goal = 8,
            resetFrequency = "Daily",
            updatedDate = twoDaysAgo
        )
        val dao = FakeTrackerDao(listOf(staleTracker))
        val repo = TrackerRepository(dao)

        // Collecting the Flow must NOT trigger database writes/history duplication
        val fromFlow = repo.allTrackersFlow.first()
        assertEquals(5, fromFlow.first().currentCount)
        assertTrue(dao.histories.isEmpty())

        // Concurrent explicit resets must serialize behind resetMutex and only record history once
        val results = (1..5).map {
            async { repo.getAllTrackersSync() }
        }.awaitAll()

        assertTrue(results.all { it.first().currentCount == 0 })
        assertEquals(1, dao.histories.size)
        assertEquals(5, dao.histories.first().value)
    }
}
