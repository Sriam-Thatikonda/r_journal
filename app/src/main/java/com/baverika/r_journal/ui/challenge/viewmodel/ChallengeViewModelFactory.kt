package com.baverika.r_journal.ui.challenge.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.baverika.r_journal.repository.ChallengeRepository

class ChallengeViewModelFactory(
    private val repository: ChallengeRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(ChallengeListViewModel::class.java) -> {
                ChallengeListViewModel(repository) as T
            }
            modelClass.isAssignableFrom(CreateChallengeViewModel::class.java) -> {
                CreateChallengeViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ChallengeDetailViewModel::class.java) -> {
                val savedStateHandle = extras.createSavedStateHandle()
                ChallengeDetailViewModel(savedStateHandle, repository) as T
            }
            modelClass.isAssignableFrom(ChallengeHistoryViewModel::class.java) -> {
                ChallengeHistoryViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
