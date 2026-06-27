package com.baverika.r_journal.ui.challenge.model

import com.baverika.r_journal.data.ChallengeStatus
import com.baverika.r_journal.data.FrequencyType
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class Challenge(
    val id: Long,
    val name: String,
    val description: String?,
    val emoji: String?,
    val totalDays: Int,
    val completedDays: Int,
    val startDate: LocalDate,
    val lastCompletedDate: LocalDate?,
    val status: ChallengeStatus,
    val createdAt: LocalDateTime,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?,
    val frequencyType: FrequencyType
) {
    val progressPercentage: Float get() = (completedDays.toFloat() / totalDays).coerceIn(0f, 1f)
    val remainingDays: Int get() = (totalDays - completedDays).coerceAtLeast(0)
    val expectedEndDate: LocalDate get() = startDate.plusDays(totalDays.toLong())
    val isCompletedToday: Boolean get() = lastCompletedDate == LocalDate.now()
    val isFinished: Boolean get() = status == ChallengeStatus.COMPLETED
}

data class ChallengeWidgetData(
    val id: Long,
    val name: String,
    val emoji: String?,
    val completedDays: Int,
    val totalDays: Int,
    val progressPercentage: Float,
    val status: ChallengeStatus,
    val isCompletedToday: Boolean
)
