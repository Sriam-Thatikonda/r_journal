package com.baverika.r_journal.data.local.dao

import androidx.room.*
import com.baverika.r_journal.data.local.entity.LifeTracker
import com.baverika.r_journal.data.local.entity.LifeTrackerEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeTrackerDao {
    // --- Trackers ---
    
    @Query("SELECT * FROM life_trackers ORDER BY createdAt DESC")
    fun getAllTrackers(): Flow<List<LifeTracker>>

    @Query("SELECT * FROM life_trackers WHERE id = :id")
    suspend fun getTrackerById(id: String): LifeTracker?

    @Query("SELECT * FROM life_trackers WHERE id = :id")
    fun getTrackerByIdFlow(id: String): Flow<LifeTracker?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: LifeTracker)

    @Delete
    suspend fun deleteTracker(tracker: LifeTracker)

    // --- Entries ---

    @Query("SELECT * FROM life_tracker_entries WHERE trackerId = :trackerId ORDER BY dateMillis ASC")
    fun getEntriesForTracker(trackerId: String): Flow<List<LifeTrackerEntry>>

    @Query("SELECT * FROM life_tracker_entries")
    fun getAllEntries(): Flow<List<LifeTrackerEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LifeTrackerEntry)

    @Delete
    suspend fun deleteEntry(entry: LifeTrackerEntry)
    
    // Helper to get latest entry (useful for UI summaries if needed)
    @Query("SELECT * FROM life_tracker_entries WHERE trackerId = :trackerId ORDER BY dateMillis DESC LIMIT 1")
    suspend fun getLatestEntry(trackerId: String): LifeTrackerEntry?
}
