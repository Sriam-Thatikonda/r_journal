package com.baverika.r_journal.ui.challenge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.repository.ChallengeRepository
import com.baverika.r_journal.ui.challenge.model.Challenge
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


sealed interface ChallengeHistoryUiState {
    data object Loading : ChallengeHistoryUiState
    data class Success(val historyChallenges: List<Challenge>) : ChallengeHistoryUiState
    data class Error(val message: String) : ChallengeHistoryUiState
}

class ChallengeHistoryViewModel constructor(
    repository: ChallengeRepository
) : ViewModel() {

    val uiState: StateFlow<ChallengeHistoryUiState> = repository.getChallengeHistory()
        .map { challenges ->
            ChallengeHistoryUiState.Success(challenges) as ChallengeHistoryUiState
        }
        .catch { e ->
            emit(ChallengeHistoryUiState.Error(e.message ?: "Unknown error occurred"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ChallengeHistoryUiState.Loading
        )
}
