package com.baverika.r_journal.data.local.dao

import androidx.room.*
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.data.local.entity.TrackerHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackerDao {
    @Query("SELECT * FROM trackers WHERE archived = 0 ORDER BY createdDate DESC")
    fun getAllTrackersFlow(): Flow<List<Tracker>>

    @Query("SELECT * FROM trackers WHERE archived = 0 ORDER BY createdDate DESC")
    fun getAllTrackers(): List<Tracker>

    @Query("SELECT * FROM trackers")
    fun getAllTrackersIncludeArchived(): List<Tracker>

    @Query("SELECT * FROM trackers WHERE id = :id")
    suspend fun getTrackerById(id: String): Tracker?

    @Query("SELECT * FROM trackers WHERE id = :id")
    fun getTrackerByIdFlow(id: String): Flow<Tracker?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracker(tracker: Tracker)

    @Update
    suspend fun updateTracker(tracker: Tracker)

    @Delete
    suspend fun deleteTracker(tracker: Tracker)

    // History Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: TrackerHistory)

    @Update
    suspend fun updateHistory(history: TrackerHistory)

    @Query("SELECT * FROM tracker_history WHERE trackerId = :trackerId ORDER BY date DESC")
    fun getHistoryForTracker(trackerId: String): Flow<List<TrackerHistory>>

    @Query("SELECT * FROM tracker_history WHERE trackerId = :trackerId AND date = :date LIMIT 1")
    suspend fun getHistoryForTrackerAndDate(trackerId: String, date: String): TrackerHistory?
}
