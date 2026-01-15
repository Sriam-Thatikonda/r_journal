package com.baverika.r_journal.quotes.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.quotes.data.QuoteEntity
import com.baverika.r_journal.quotes.data.QuoteRepository
import com.baverika.r_journal.quotes.widget.QuotesWidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI State for the Quotes screen
 */
data class QuotesUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val editingQuote: QuoteEntity? = null,
    val showAddEditDialog: Boolean = false
)

/**
 * ViewModel for managing quotes.
 * Handles CRUD operations and widget updates.
 */
class QuotesViewModel(
    private val repository: QuoteRepository,
    private val context: Context
) : ViewModel() {

    // All quotes (including inactive) - for display in list
    val allQuotes: StateFlow<List<QuoteEntity>> = repository.getAllQuotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State
    private val _uiState = MutableStateFlow(QuotesUiState())
    val uiState: StateFlow<QuotesUiState> = _uiState.asStateFlow()

    /**
     * Add a new quote
     */
    fun addQuote(text: String, author: String?) {
        if (text.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Quote text cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val quote = QuoteEntity(
                    text = text.trim(),
                    author = author?.trim()?.takeIf { it.isNotBlank() }
                )
                repository.insertQuote(quote)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddEditDialog = false,
                    errorMessage = null
                )
                // Update widget after adding a quote
                QuotesWidgetUpdater.updateWidget(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to add quote: ${e.message}"
                )
            }
        }
    }

    /**
     * Update an existing quote
     */
    fun updateQuote(quote: QuoteEntity, newText: String, newAuthor: String?) {
        if (newText.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Quote text cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val updatedQuote = quote.copy(
                    text = newText.trim(),
                    author = newAuthor?.trim()?.takeIf { it.isNotBlank() }
                )
                repository.updateQuote(updatedQuote)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    showAddEditDialog = false,
                    editingQuote = null,
                    errorMessage = null
                )
                // Update widget after editing a quote
                QuotesWidgetUpdater.updateWidget(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to update quote: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggle quote active status (soft delete/restore)
     */
    fun toggleQuoteActive(quote: QuoteEntity) {
        viewModelScope.launch {
            try {
                repository.toggleQuoteActive(quote.id, !quote.isActive)
                // Update widget when quote status changes
                QuotesWidgetUpdater.updateWidget(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update quote: ${e.message}"
                )
            }
        }
    }

    /**
     * Permanently delete a quote
     */
    fun deleteQuote(quote: QuoteEntity) {
        viewModelScope.launch {
            try {
                repository.deleteQuote(quote)
                // Update widget after deletion
                QuotesWidgetUpdater.updateWidget(context)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete quote: ${e.message}"
                )
            }
        }
    }

    /**
     * Show add dialog
     */
    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingQuote = null,
            errorMessage = null
        )
    }

    /**
     * Show edit dialog for a specific quote
     */
    fun showEditDialog(quote: QuoteEntity) {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = true,
            editingQuote = quote,
            errorMessage = null
        )
    }

    /**
     * Dismiss the add/edit dialog
     */
    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(
            showAddEditDialog = false,
            editingQuote = null,
            errorMessage = null
        )
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Manually refresh the widget
     */
    fun refreshWidget() {
        viewModelScope.launch {
            QuotesWidgetUpdater.updateWidget(context)
        }
    }
}

/**
 * Factory for creating QuotesViewModel
 */
class QuotesViewModelFactory(
    private val repository: QuoteRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuotesViewModel::class.java)) {
            return QuotesViewModel(repository, context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
