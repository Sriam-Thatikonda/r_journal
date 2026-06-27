package com.baverika.r_journal.ui.challenge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.ChallengeStatus
import com.baverika.r_journal.data.FrequencyType
import com.baverika.r_journal.repository.ChallengeRepository
import com.baverika.r_journal.ui.challenge.model.Challenge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime


data class CreateChallengeUiState(
    val name: String = "",
    val description: String = "",
    val totalDays: String = "",
    val emoji: String = "🎯",
    val startDate: LocalDate = LocalDate.now(),
    val reminderEnabled: Boolean = false,
    val reminderTime: LocalTime? = null,
    val isSaving: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && totalDays.toIntOrNull() != null && totalDays.toIntOrNull()!! > 0
}

sealed interface CreateChallengeUiEvent {
    data class ChallengeCreated(val challengeId: Long) : CreateChallengeUiEvent
    data class ShowError(val message: String) : CreateChallengeUiEvent
}

class CreateChallengeViewModel constructor(
    private val repository: ChallengeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateChallengeUiState())
    val uiState: StateFlow<CreateChallengeUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CreateChallengeUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun updateName(name: String) {
        _uiState.update { it.copy(name = name) }
    }

    fun updateDescription(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun updateTotalDays(days: String) {
        _uiState.update { it.copy(totalDays = days) }
    }

    fun updateEmoji(emoji: String) {
        _uiState.update { it.copy(emoji = emoji) }
    }

    fun updateStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
    }

    fun updateReminder(enabled: Boolean, time: LocalTime? = null) {
        _uiState.update { it.copy(reminderEnabled = enabled, reminderTime = time) }
    }

    fun saveChallenge() {
        val state = _uiState.value
        if (!state.isValid) {
            viewModelScope.launch {
                _uiEvent.emit(CreateChallengeUiEvent.ShowError("Please fill in all required fields properly."))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val challenge = Challenge(
                    id = 0, // Auto-generated
                    name = state.name.trim(),
                    description = state.description.trim().takeIf { it.isNotBlank() },
                    emoji = state.emoji,
                    totalDays = state.totalDays.toInt(),
                    completedDays = 0,
                    startDate = state.startDate,
                    lastCompletedDate = null,
                    status = ChallengeStatus.ACTIVE,
                    createdAt = LocalDateTime.now(),
                    reminderEnabled = state.reminderEnabled,
                    reminderTime = state.reminderTime,
                    frequencyType = FrequencyType.DAILY
                )
                val id = repository.insertChallenge(challenge)
                _uiEvent.emit(CreateChallengeUiEvent.ChallengeCreated(id))
            } catch (e: Exception) {
                _uiEvent.emit(CreateChallengeUiEvent.ShowError(e.message ?: "Failed to save challenge"))
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
