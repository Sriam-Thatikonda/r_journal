package com.baverika.r_journal.ui.challenge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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


sealed interface ChallengeListUiState {
    data object Loading : ChallengeListUiState
    data class Success(val activeChallenges: List<Challenge>) : ChallengeListUiState
    data class Error(val message: String) : ChallengeListUiState
}

sealed interface ChallengeListUiEvent {
    data class ShowSnackbar(val message: String) : ChallengeListUiEvent
}

class ChallengeListViewModel constructor(
    private val repository: ChallengeRepository
) : ViewModel() {

    private val _uiEvent = MutableSharedFlow<ChallengeListUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    val uiState: StateFlow<ChallengeListUiState> = repository.getActiveChallenges()
        .map { challenges ->
            ChallengeListUiState.Success(challenges) as ChallengeListUiState
        }
        .catch { e ->
            emit(ChallengeListUiState.Error(e.message ?: "Unknown error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChallengeListUiState.Loading
        )

    fun markChallengeCompleteToday(challengeId: Long) {
        viewModelScope.launch {
            try {
                repository.markTodayComplete(challengeId)
                _uiEvent.emit(ChallengeListUiEvent.ShowSnackbar("Challenge marked as completed for today! 🎉"))
            } catch (e: AlreadyCompletedTodayException) {
                _uiEvent.emit(ChallengeListUiEvent.ShowSnackbar("Already completed today."))
            } catch (e: Exception) {
                _uiEvent.emit(ChallengeListUiEvent.ShowSnackbar("Error: ${e.message}"))
            }
        }
    }
}
