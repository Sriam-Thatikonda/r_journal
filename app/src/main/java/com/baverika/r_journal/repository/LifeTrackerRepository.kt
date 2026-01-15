package com.baverika.r_journal.repository

import com.baverika.r_journal.data.local.dao.LifeTrackerDao
import com.baverika.r_journal.data.local.entity.LifeTracker
import com.baverika.r_journal.data.local.entity.LifeTrackerEntry
import kotlinx.coroutines.flow.Flow

class LifeTrackerRepository(private val dao: LifeTrackerDao) {

    val allTrackers: Flow<List<LifeTracker>> = dao.getAllTrackers()

    fun getTracker(id: String): Flow<LifeTracker?> = dao.getTrackerByIdFlow(id)

    fun getEntries(trackerId: String): Flow<List<LifeTrackerEntry>> = dao.getEntriesForTracker(trackerId)
    
    val allEntries: Flow<List<LifeTrackerEntry>> = dao.getAllEntries()

    suspend fun insertTracker(tracker: LifeTracker) = dao.insertTracker(tracker)
    suspend fun deleteTracker(tracker: LifeTracker) = dao.deleteTracker(tracker)

    suspend fun insertEntry(entry: LifeTrackerEntry) = dao.insertEntry(entry)
    suspend fun deleteEntry(entry: LifeTrackerEntry) = dao.deleteEntry(entry)
}
