package com.baverika.r_journal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.repository.EventRepository
import com.baverika.r_journal.repository.TaskRepository
import com.baverika.r_journal.utils.EventTaskGenerator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class EventViewModel(
    private val repository: EventRepository,
    private val taskRepository: TaskRepository? = null,
    private val context: Context? = null
) : ViewModel() {

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
            if (taskRepository != null) {
                val events = repository.getAllEventsList()
                EventTaskGenerator.ensureEventTasksCreated(events, taskRepository, context)
            } else if (context != null) {
                EventTaskGenerator.ensureEventTasksCreated(context)
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            repository.updateEvent(event)
            if (taskRepository != null) {
                val events = repository.getAllEventsList()
                EventTaskGenerator.ensureEventTasksCreated(events, taskRepository, context)
            } else if (context != null) {
                EventTaskGenerator.ensureEventTasksCreated(context)
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            repository.deleteEvent(event)
        }
    }

    private fun safeEventDate(year: Int, month: Int, day: Int): LocalDate {
        return if (month == 2 && day == 29 && !java.time.Year.isLeap(year.toLong())) {
            LocalDate.of(year, 2, 28)
        } else {
            LocalDate.of(year, month, day)
        }
    }

    private fun calculateDaysRemaining(day: Int, month: Int): Long {
        val today = LocalDate.now()
        val currentYear = today.year
        var eventDate = safeEventDate(currentYear, month, day)

        if (eventDate.isBefore(today)) {
            eventDate = safeEventDate(currentYear + 1, month, day)
        }

        return ChronoUnit.DAYS.between(today, eventDate)
    }
}

data class EventUiModel(
    val event: Event,
    val daysRemaining: Long
)

class EventViewModelFactory(
    private val repository: EventRepository,
    private val taskRepository: TaskRepository? = null,
    private val context: Context? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EventViewModel(repository, taskRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
