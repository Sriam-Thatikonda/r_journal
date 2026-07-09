package com.baverika.r_journal.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val emoji: String?,
    val totalDays: Int,
    val completedDays: Int,
    val startDate: LocalDate,
    val lastCompletedDate: LocalDate?,
    val status: ChallengeStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    val frequencyType: FrequencyType,
    val linkedJournalEntryId: Long?
)

enum class ChallengeStatus { ACTIVE, COMPLETED, ABANDONED, ARCHIVED }
enum class FrequencyType { DAILY }

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE status = 'ACTIVE' ORDER BY createdAt DESC")
    fun getAllActiveChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE status IN ('COMPLETED', 'ARCHIVED', 'ABANDONED') ORDER BY createdAt DESC")
    fun getCompletedAndArchivedChallenges(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges WHERE id = :id")
    fun getChallengeById(id: Long): Flow<ChallengeEntity?>

    @Query("SELECT * FROM challenges ORDER BY createdAt DESC")
    fun getAllChallengesFlow(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges")
    suspend fun getAllChallenges(): List<ChallengeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(entity: ChallengeEntity): Long

    @Update
    suspend fun updateChallenge(entity: ChallengeEntity)

    @Delete
    suspend fun deleteChallenge(entity: ChallengeEntity)
}
