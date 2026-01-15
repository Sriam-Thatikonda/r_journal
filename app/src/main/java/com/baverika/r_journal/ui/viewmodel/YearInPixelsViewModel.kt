package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.repository.JournalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class YearInPixelsViewModel(
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _moodMap = MutableStateFlow<Map<LocalDate, String>>(emptyMap())
    val moodMap: StateFlow<Map<LocalDate, String>> = _moodMap.asStateFlow()

    private val _selectedYear = MutableStateFlow(LocalDate.now().year)
    val selectedYear: StateFlow<Int> = _selectedYear.asStateFlow()

    init {
        loadMoods()
    }

    private fun loadMoods() {
        viewModelScope.launch {
            journalRepository.allEntries.collectLatest { entries ->
                val map = entries.associate { entry ->
                    val date = java.time.Instant.ofEpochMilli(entry.dateMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    
                    // Prefer explicitly set mood, fallback to tag
                    var mood = entry.mood
                    if (!mood.isNullOrEmpty()) {
                        // Handle comma-separated moods (take LAST one to show most recent selection)
                        mood = mood.split(",").lastOrNull()?.trim()
                    }
                    
                    if (mood.isNullOrEmpty()) {
                        mood = entry.tags.lastOrNull { it.startsWith("#mood-") }
                            ?.removePrefix("#mood-")
                    }
                    
                    // Capitalize first letter to match MoodColors keys e.g. "happy" -> "Happy"
                    mood = mood?.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
                    
                    date to (mood ?: "")
                }.filterValues { it.isNotEmpty() }
                _moodMap.value = map
            }
        }
    }

    fun setMood(date: LocalDate, mood: String) {
        viewModelScope.launch {
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            val entry = journalRepository.getOrCreateEntryForDate(dateMillis)
            
            // Update tags to include mood tag for consistency with existing stats
            val moodTag = "#mood-${mood.lowercase()}"
            val otherTags = entry.tags.filter { !it.startsWith("#mood-") }
            val updatedTags = otherTags + moodTag
            
            val updatedEntry = entry.copy(
                mood = mood,
                tags = updatedTags
            )
            journalRepository.upsertEntry(updatedEntry)
        }
    }

    fun updateYear(year: Int) {
        _selectedYear.value = year
    }
}

class YearInPixelsViewModelFactory(private val repository: JournalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(YearInPixelsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return YearInPixelsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
