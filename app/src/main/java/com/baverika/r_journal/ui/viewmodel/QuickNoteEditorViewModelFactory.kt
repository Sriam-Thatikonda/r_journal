package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baverika.r_journal.data.model.NoteColor
import com.baverika.r_journal.repository.QuickNoteRepository

class QuickNoteEditorViewModelFactory(
    private val repository: QuickNoteRepository,
    private val initialNoteId: String?,
    private val defaultNoteColor: Long = NoteColor.DEFAULT.dotColor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickNoteEditorViewModel::class.java)) {
            return QuickNoteEditorViewModel(repository, initialNoteId, defaultNoteColor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
