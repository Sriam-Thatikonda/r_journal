package com.baverika.r_journal.data.local.dao

import androidx.room.*
import com.baverika.r_journal.data.local.entity.CravingLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CravingLogDao {
    @Query("SELECT * FROM craving_logs ORDER BY createdAt DESC")
    fun getAllLogs(): Flow<List<CravingLogEntity>>

    @Query("SELECT * FROM craving_logs WHERE id = :id")
    suspend fun getLogById(id: String): CravingLogEntity?

    @Query("SELECT COUNT(*) FROM craving_logs WHERE createdAt >= :startOfDay")
    suspend fun getLogCountToday(startOfDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: CravingLogEntity)

    @Update
    suspend fun updateLog(log: CravingLogEntity)

    @Delete
    suspend fun deleteLog(log: CravingLogEntity)
}
