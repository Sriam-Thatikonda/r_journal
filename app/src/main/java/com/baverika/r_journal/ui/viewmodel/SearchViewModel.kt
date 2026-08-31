package com.baverika.r_journal.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.JournalEntry
import com.baverika.r_journal.data.local.entity.QuickNote
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.quotes.data.QuoteEntity
import com.baverika.r_journal.quotes.data.QuoteRepository
import com.baverika.r_journal.repository.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

enum class SearchCategory(val label: String) {
    ALL("All"),
    JOURNAL("Journals"),
    NOTES("Notes"),
    TASKS("Tasks"),
    TRACKERS("Trackers"),
    QUOTES("Quotes")
}

sealed class UnifiedSearchResult {
    abstract val id: String
    abstract val title: String
    abstract val subtitle: String
    abstract val category: SearchCategory

    data class JournalResult(
        val entry: JournalEntry,
        override val id: String = entry.id,
        override val title: String = entry.localDate.toString(),
        override val subtitle: String = entry.messages.firstOrNull { it.content.isNotBlank() }?.content ?: "Journal Entry",
        override val category: SearchCategory = SearchCategory.JOURNAL
    ) : UnifiedSearchResult()

    data class NoteResult(
        val note: QuickNote,
        override val id: String = note.id,
        override val title: String = note.title.ifBlank { "Untitled Note" },
        override val subtitle: String = note.content,
        override val category: SearchCategory = SearchCategory.NOTES
    ) : UnifiedSearchResult()

    data class TaskResult(
        val task: Task,
        override val id: String = task.id,
        override val title: String = task.title,
        override val subtitle: String = task.description ?: "",
        override val category: SearchCategory = SearchCategory.TASKS
    ) : UnifiedSearchResult()

    data class TrackerResult(
        val tracker: Tracker,
        override val id: String = tracker.id,
        override val title: String = "${tracker.emoji} ${tracker.title}",
        override val subtitle: String = "${tracker.currentCount} / ${tracker.goal}",
        override val category: SearchCategory = SearchCategory.TRACKERS
    ) : UnifiedSearchResult()

    data class QuoteResult(
        val quote: QuoteEntity,
        override val id: String = quote.id.toString(),
        override val title: String = quote.text,
        override val subtitle: String = quote.author ?: "Unknown Author",
        override val category: SearchCategory = SearchCategory.QUOTES
    ) : UnifiedSearchResult()
}

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val journalRepo: JournalRepository,
    private val quickNoteRepo: QuickNoteRepository,
    private val taskRepo: TaskRepository,
    private val trackerRepo: TrackerRepository,
    private val quoteRepo: QuoteRepository,
    private val context: Context
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedCategory = MutableStateFlow(SearchCategory.ALL)
    val selectedCategory: StateFlow<SearchCategory> = _selectedCategory.asStateFlow()

    val searchResults: StateFlow<List<UnifiedSearchResult>> = combine(
        _query.debounce(300).distinctUntilChanged(),
        _selectedCategory,
        journalRepo.allEntries,
        quickNoteRepo.allNotes,
        taskRepo.allTasks,
        trackerRepo.allTrackersFlow,
        quoteRepo.getAllQuotes()
    ) { arrayOfFlows ->
        val queryStr = arrayOfFlows[0] as String
        val category = arrayOfFlows[1] as SearchCategory
        @Suppress("UNCHECKED_CAST")
        val journals = arrayOfFlows[2] as List<JournalEntry>
        @Suppress("UNCHECKED_CAST")
        val notes = arrayOfFlows[3] as List<QuickNote>
        @Suppress("UNCHECKED_CAST")
        val tasks = arrayOfFlows[4] as List<Task>
        @Suppress("UNCHECKED_CAST")
        val trackers = arrayOfFlows[5] as List<Tracker>
        @Suppress("UNCHECKED_CAST")
        val quotes = arrayOfFlows[6] as List<QuoteEntity>

        if (queryStr.isBlank()) {
            emptyList()
        } else {
            val results = mutableListOf<UnifiedSearchResult>()
            val q = queryStr.lowercase()

            // 1. Journal Search
            if (category == SearchCategory.ALL || category == SearchCategory.JOURNAL) {
                journals.filter { entry ->
                    entry.messages.any { it.content.lowercase().contains(q) } ||
                    entry.tags.any { it.lowercase().contains(q) }
                }.forEach { results.add(UnifiedSearchResult.JournalResult(it)) }
            }

            // 2. Notes Search
            if (category == SearchCategory.ALL || category == SearchCategory.NOTES) {
                notes.filter { note ->
                    note.title.lowercase().contains(q) || note.content.lowercase().contains(q)
                }.forEach { results.add(UnifiedSearchResult.NoteResult(it)) }
            }

            // 3. Tasks Search
            if (category == SearchCategory.ALL || category == SearchCategory.TASKS) {
                tasks.filter { task ->
                    task.title.lowercase().contains(q) || (task.description?.lowercase()?.contains(q) == true)
                }.forEach { results.add(UnifiedSearchResult.TaskResult(it)) }
            }

            // 4. Trackers Search
            if (category == SearchCategory.ALL || category == SearchCategory.TRACKERS) {
                trackers.filter { tracker ->
                    tracker.title.lowercase().contains(q)
                }.forEach { results.add(UnifiedSearchResult.TrackerResult(it)) }
            }

            // 5. Quotes Search
            if (category == SearchCategory.ALL || category == SearchCategory.QUOTES) {
                quotes.filter { quote ->
                    quote.text.lowercase().contains(q) || (quote.author?.lowercase()?.contains(q) == true)
                }.forEach { results.add(UnifiedSearchResult.QuoteResult(it)) }
            }

            results
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }

    fun selectCategory(category: SearchCategory) {
        _selectedCategory.value = category
    }
}