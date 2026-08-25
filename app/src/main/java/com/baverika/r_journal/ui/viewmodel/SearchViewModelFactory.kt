// app/src/main/java/com/baverika/r_journal/ui/viewmodel/SearchViewModelFactory.kt

package com.baverika.r_journal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baverika.r_journal.repository.JournalRepository

class SearchViewModelFactory(
    private val journalRepo: JournalRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(journalRepo, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}