package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.QuickNotesPreferences
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.repository.QuickNoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuickNoteViewModel(
    private val repository: QuickNoteRepository,
    private val preferences: QuickNotesPreferences
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Layout type preference
    val layoutType: StateFlow<String> = preferences.layoutType.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = QuickNotesPreferences.LAYOUT_MASONRY
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allNotes = _searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.allNotes
        } else {
            repository.searchNotes(query)
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setLayoutType(layoutType: String) {
        viewModelScope.launch {
            preferences.saveLayoutType(layoutType)
        }
    }

    fun addNote(title: String, content: String, color: Long = 0xFFFFFFFF) {
        if (title.isBlank() && content.isBlank()) return
        val note = QuickNote(
            title = title.ifBlank { "Untitled" },
            content = content,
            color = color
        )
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }

    fun updateNote(note: QuickNote) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }

    fun deleteNote(note: QuickNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
}