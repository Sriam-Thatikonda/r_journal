package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.LifeTracker
import com.baverika.r_journal.data.local.entity.LifeTrackerEntry
import com.baverika.r_journal.repository.LifeTrackerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TrackerUiModel(
    val tracker: LifeTracker,
    val lastEntry: LifeTrackerEntry?
)

class LifeTrackerViewModel(private val repository: LifeTrackerRepository) : ViewModel() {
    
    val trackers = combine(
        repository.allTrackers,
        repository.allEntries
    ) { trackers, entries ->
        val entriesByTracker = entries.groupBy { it.trackerId }
        trackers.map { tracker ->
            val last = entriesByTracker[tracker.id]?.maxByOrNull { it.dateMillis }
            TrackerUiModel(tracker, last)
        }
        .sortedWith(
            compareByDescending<TrackerUiModel> { it.lastEntry?.dateMillis ?: 0L }
                .thenByDescending { it.tracker.createdAt }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun createTracker(name: String, icon: String, color: Long) {
        viewModelScope.launch {
            repository.insertTracker(
                LifeTracker(
                    name = name,
                    icon = icon,
                    color = color
                )
            )
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


class LifeTrackerViewModelFactory(private val repository: LifeTrackerRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LifeTrackerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LifeTrackerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
