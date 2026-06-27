package com.baverika.r_journal.ui.challenge.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.ChallengeStatus
import com.baverika.r_journal.repository.ChallengeRepository
import com.baverika.r_journal.repository.AlreadyCompletedTodayException
import com.baverika.r_journal.ui.challenge.model.Challenge
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


sealed interface ChallengeDetailUiState {
    data object Loading : ChallengeDetailUiState
    data class Success(val challenge: Challenge) : ChallengeDetailUiState
    data class Error(val message: String) : ChallengeDetailUiState
}

sealed interface ChallengeDetailUiEvent {
    data class ShowSnackbar(val message: String) : ChallengeDetailUiEvent
    data object NavigateBack : ChallengeDetailUiEvent
}

class ChallengeDetailViewModel constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ChallengeRepository
) : ViewModel() {

    private val challengeId: Long = checkNotNull(savedStateHandle["challengeId"])

    private val _uiEvent = MutableSharedFlow<ChallengeDetailUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val uiState: StateFlow<ChallengeDetailUiState> = repository.getChallengeById(challengeId)
        .map { challenge ->
            if (challenge != null) {
                ChallengeDetailUiState.Success(challenge) as ChallengeDetailUiState
            } else {
                ChallengeDetailUiState.Error("Challenge not found") as ChallengeDetailUiState
            }
        }
        .catch { e ->
            emit(ChallengeDetailUiState.Error(e.message ?: "Unknown error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChallengeDetailUiState.Loading
        )

    fun markChallengeCompleteToday() {
        viewModelScope.launch {
            try {
                repository.markTodayComplete(challengeId)
                _uiEvent.emit(ChallengeDetailUiEvent.ShowSnackbar("Challenge marked as completed for today! 🎉"))
            } catch (e: AlreadyCompletedTodayException) {
                _uiEvent.emit(ChallengeDetailUiEvent.ShowSnackbar("Already completed today."))
            } catch (e: Exception) {
                _uiEvent.emit(ChallengeDetailUiEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }

    fun abandonChallenge() {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState is ChallengeDetailUiState.Success) {
                    val updatedChallenge = currentState.challenge.copy(status = ChallengeStatus.ABANDONED)
                    repository.updateChallenge(updatedChallenge)
                    _uiEvent.emit(ChallengeDetailUiEvent.ShowSnackbar("Challenge abandoned."))
                    _uiEvent.emit(ChallengeDetailUiEvent.NavigateBack)
                }
            } catch (e: Exception) {
                _uiEvent.emit(ChallengeDetailUiEvent.ShowSnackbar("Failed to abandon challenge."))
            }
        }
    }

    fun deleteChallenge() {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState is ChallengeDetailUiState.Success) {
                    repository.deleteChallenge(currentState.challenge)
                    _uiEvent.emit(ChallengeDetailUiEvent.ShowSnackbar("Challenge deleted."))
                    _uiEvent.emit(ChallengeDetailUiEvent.NavigateBack)
                }
            } catch (e: Exception) {
                _uiEvent.emit(ChallengeDetailUiEvent.ShowSnackbar("Failed to delete challenge."))
            }
        }
    }
}
