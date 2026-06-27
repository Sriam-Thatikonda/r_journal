package com.baverika.r_journal.repository

import com.baverika.r_journal.data.ChallengeDao
import com.baverika.r_journal.data.ChallengeEntity
import com.baverika.r_journal.data.ChallengeStatus
import com.baverika.r_journal.ui.challenge.model.Challenge
import com.baverika.r_journal.ui.challenge.model.ChallengeWidgetData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime

class AlreadyCompletedTodayException : Exception("Challenge is already completed for today.")

class ChallengeRepository(
    private val dao: ChallengeDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    fun getActiveChallenges(): Flow<List<Challenge>> {
        return dao.getAllActiveChallenges().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getChallengeHistory(): Flow<List<Challenge>> {
        return dao.getCompletedAndArchivedChallenges().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    fun getChallengeById(id: Long): Flow<Challenge?> {
        return dao.getChallengeById(id).map { it?.toDomainModel() }
    }

    suspend fun insertChallenge(challenge: Challenge): Long = withContext(ioDispatcher) {
        dao.insertChallenge(challenge.toEntity())
    }

    suspend fun updateChallenge(challenge: Challenge) = withContext(ioDispatcher) {
        dao.updateChallenge(challenge.toEntity())
    }
    
    suspend fun deleteChallenge(challenge: Challenge) = withContext(ioDispatcher) {
        dao.deleteChallenge(challenge.toEntity())
    }

    suspend fun markTodayComplete(challengeId: Long) = withContext(ioDispatcher) {
        val entity = dao.getChallengeById(challengeId).firstOrNull() ?: return@withContext
        val today = LocalDate.now()
        
        if (entity.lastCompletedDate == today) {
            throw AlreadyCompletedTodayException()
        }

        val newCompletedDays = entity.completedDays + 1
        var newStatus = entity.status
        
        if (newCompletedDays >= entity.totalDays) {
            newStatus = ChallengeStatus.COMPLETED
        }

        val updatedEntity = entity.copy(
            completedDays = newCompletedDays,
            lastCompletedDate = today,
            status = newStatus,
            updatedAt = LocalDateTime.now()
        )
        
        dao.updateChallenge(updatedEntity)
    }
    
    fun getWidgetData(): Flow<List<ChallengeWidgetData>> {
        // TODO: Bind to GlanceAppWidget here
        return dao.getAllActiveChallenges().map { entities ->
            entities.map { entity ->
                val progressPercentage = (entity.completedDays.toFloat() / entity.totalDays).coerceIn(0f, 1f)
                val isCompletedToday = entity.lastCompletedDate == LocalDate.now()
                ChallengeWidgetData(
                    id = entity.id,
                    name = entity.name,
                    emoji = entity.emoji,
                    completedDays = entity.completedDays,
                    totalDays = entity.totalDays,
                    progressPercentage = progressPercentage,
                    status = entity.status,
                    isCompletedToday = isCompletedToday
                )
            }
        }
    }

    private fun ChallengeEntity.toDomainModel(): Challenge {
        return Challenge(
            id = id,
            name = name,
            description = description,
            emoji = emoji,
            totalDays = totalDays,
            completedDays = completedDays,
            startDate = startDate,
            lastCompletedDate = lastCompletedDate,
            status = status,
            createdAt = createdAt,
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime,
            frequencyType = frequencyType
        )
    }

    private fun Challenge.toEntity(): ChallengeEntity {
        return ChallengeEntity(
            id = id,
            name = name,
            description = description,
            emoji = emoji,
            totalDays = totalDays,
            completedDays = completedDays,
            startDate = startDate,
            lastCompletedDate = lastCompletedDate,
            status = status,
            createdAt = createdAt,
            updatedAt = LocalDateTime.now(),
            reminderEnabled = reminderEnabled,
            reminderTime = reminderTime,
            frequencyType = frequencyType,
            linkedJournalEntryId = null // Reserved for future use
        )
    }
}
