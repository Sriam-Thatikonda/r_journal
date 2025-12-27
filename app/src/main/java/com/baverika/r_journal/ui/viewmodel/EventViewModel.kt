package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.repository.EventRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class EventViewModel(private val repository: EventRepository) : ViewModel() {

    val allEvents: StateFlow<List<EventUiModel>> = repository.allEvents
        .map { events ->
            events.map { event ->
                val daysRemaining = calculateDaysRemaining(event.day, event.month)
                EventUiModel(event, daysRemaining)
            }.sortedBy { it.daysRemaining }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addEvent(event: Event) {
        viewModelScope.launch {
            repository.insertEvent(event)
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    private fun calculateDaysRemaining(day: Int, month: Int): Long {
        val today = LocalDate.now()
        val currentYear = today.year
        var eventDate = LocalDate.of(currentYear, month, day)

        if (eventDate.isBefore(today) || eventDate.isEqual(today)) {
            eventDate = eventDate.plusYears(1)
        }

        return ChronoUnit.DAYS.between(today, eventDate)
    }
}

data class EventUiModel(
    val event: Event,
    val daysRemaining: Long
)

class EventViewModelFactory(private val repository: EventRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
