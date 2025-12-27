// app/src/main/java/com/baverika/r_journal/ui/viewmodel/QuickNoteViewModelFactory.kt

package com.baverika.r_journal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baverika.r_journal.data.local.QuickNotesPreferences
import com.baverika.r_journal.repository.QuickNoteRepository

class QuickNoteViewModelFactory(
    private val repository: QuickNoteRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuickNoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val preferences = QuickNotesPreferences(context)
            return QuickNoteViewModel(repository, preferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}