package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.LifeTracker
import com.baverika.r_journal.data.local.entity.LifeTrackerEntry
import com.baverika.r_journal.repository.LifeTrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackerDetailViewModel(
    private val repository: LifeTrackerRepository,
    private val trackerId: String
) : ViewModel() {

    val tracker: StateFlow<LifeTracker?> = repository.getTracker(trackerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val entries = repository.getEntries(trackerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addEntry(dateMillis: Long, note: String) {
        viewModelScope.launch {
            repository.insertEntry(
                LifeTrackerEntry(
                    trackerId = trackerId,
                    dateMillis = dateMillis,
                    note = note
                )
            )
        }
    }

    fun updateEntry(entry: LifeTrackerEntry) {
        viewModelScope.launch {
            repository.insertEntry(entry)
        }
    }

    fun deleteEntry(entry: LifeTrackerEntry) {
        viewModelScope.launch {
            repository.deleteEntry(entry)
        }
    }

    fun updateTracker(tracker: LifeTracker) {
        viewModelScope.launch {
            repository.insertTracker(tracker)
        }
    }

    fun deleteTracker(tracker: LifeTracker) {
        viewModelScope.launch {
            repository.deleteTracker(tracker)
        }
    }
}

class TrackerDetailViewModelFactory(
    private val repository: LifeTrackerRepository,
    private val trackerId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrackerDetailViewModel(repository, trackerId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
