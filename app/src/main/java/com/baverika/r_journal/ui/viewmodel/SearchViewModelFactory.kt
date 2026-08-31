package com.baverika.r_journal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.baverika.r_journal.quotes.data.QuoteRepository
import com.baverika.r_journal.repository.*

class SearchViewModelFactory(
    private val journalRepo: JournalRepository,
    private val quickNoteRepo: QuickNoteRepository,
    private val taskRepo: TaskRepository,
    private val trackerRepo: TrackerRepository,
    private val quoteRepo: QuoteRepository,
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
            return SearchViewModel(
                journalRepo,
                quickNoteRepo,
                taskRepo,
                trackerRepo,
                quoteRepo,
                context
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}