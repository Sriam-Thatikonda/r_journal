package com.baverika.r_journal.repository

import com.baverika.r_journal.data.local.dao.TrackerDao
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.data.local.entity.TrackerHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*

class TrackerRepository(private val dao: TrackerDao) {

    val allTrackersFlow: Flow<List<Tracker>> = dao.getAllTrackersFlow().map { list ->
        list.map { verifyAndResetTracker(it) }
    }

    suspend fun getAllTrackersSync(): List<Tracker> {
        return dao.getAllTrackers().map { verifyAndResetTracker(it) }
    }

    fun getTrackerByIdFlow(id: String): Flow<Tracker?> = dao.getTrackerByIdFlow(id).map { tracker ->
        tracker?.let { verifyAndResetTracker(it) }
    }

    suspend fun getTrackerById(id: String): Tracker? {
        return dao.getTrackerById(id)?.let { verifyAndResetTracker(it) }
    }

    suspend fun insertTracker(tracker: Tracker) = dao.insertTracker(tracker)

    suspend fun updateTracker(tracker: Tracker) = dao.updateTracker(tracker)

    suspend fun deleteTracker(tracker: Tracker) = dao.deleteTracker(tracker)

    fun getHistoryForTracker(trackerId: String): Flow<List<TrackerHistory>> = dao.getHistoryForTracker(trackerId)

    suspend fun incrementTracker(trackerId: String, amount: Int): Tracker? {
        val tracker = getTrackerById(trackerId) ?: return null
        val updated = tracker.copy(
            currentCount = tracker.currentCount + amount,
            updatedDate = System.currentTimeMillis()
        )
        dao.updateTracker(updated)
        return updated
    }

    suspend fun decrementTracker(trackerId: String, amount: Int): Tracker? {
        val tracker = getTrackerById(trackerId) ?: return null
        val updated = tracker.copy(
            currentCount = (tracker.currentCount - amount).coerceAtLeast(0),
            updatedDate = System.currentTimeMillis()
        )
        dao.updateTracker(updated)
        return updated
    }

    suspend fun resetTracker(trackerId: String): Tracker? {
        val tracker = getTrackerById(trackerId) ?: return null
        val updated = tracker.copy(
            currentCount = 0,
            updatedDate = System.currentTimeMillis()
        )
        dao.updateTracker(updated)
        return updated
    }

    // Inner helper to verify and reset if needed
    private suspend fun verifyAndResetTracker(tracker: Tracker): Tracker {
        val now = System.currentTimeMillis()
        val updatedCal = Calendar.getInstance().apply { timeInMillis = tracker.updatedDate }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        var needsReset = false
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val lastDateStr = sdf.format(Date(tracker.updatedDate))

        when (tracker.resetFrequency) {
            "Daily" -> {
                val isSameDay = updatedCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        updatedCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
                if (!isSameDay) {
                    needsReset = true
                }
            }
            "Weekly" -> {
                val isSameWeek = updatedCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        updatedCal.get(Calendar.WEEK_OF_YEAR) == nowCal.get(Calendar.WEEK_OF_YEAR)
                if (!isSameWeek) {
                    needsReset = true
                }
            }
        }

        if (needsReset) {
            // Save to history
            val historyEntry = TrackerHistory(
                trackerId = tracker.id,
                date = lastDateStr,
                value = tracker.currentCount
            )
            val existing = dao.getHistoryForTrackerAndDate(tracker.id, lastDateStr)
            if (existing != null) {
                dao.updateHistory(existing.copy(value = existing.value + tracker.currentCount))
            } else {
                dao.insertHistory(historyEntry)
            }

            // Update tracker
            val resetTracker = tracker.copy(
                currentCount = 0,
                updatedDate = now
            )
            dao.updateTracker(resetTracker)
            return resetTracker
        }
        return tracker
    }
}
