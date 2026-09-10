package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baverika.r_journal.repository.QuickNoteRepository

class QuickNoteEditorViewModelFactory(
    private val repository: QuickNoteRepository,
    private val initialNoteId: String?,
    private val defaultNoteColor: Long = 0xFF000000
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickNoteEditorViewModel::class.java)) {
            return QuickNoteEditorViewModel(repository, initialNoteId, defaultNoteColor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
