package com.baverika.r_journal.quotes.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository for Quote operations.
 * Provides a clean API layer between the data source and the rest of the app.
 */
class QuoteRepository(private val quoteDao: QuoteDao) {

    /**
     * Get all quotes as a Flow (reactive)
     */
    fun getAllQuotes(): Flow<List<QuoteEntity>> = quoteDao.getAllQuotes()

    /**
     * Get only active quotes as a Flow (reactive)
     */
    fun getActiveQuotes(): Flow<List<QuoteEntity>> = quoteDao.getActiveQuotes()

    /**
     * Get active quotes synchronously (for widget/worker use)
     */
    suspend fun getActiveQuotesSync(): List<QuoteEntity> = quoteDao.getActiveQuotesSync()

    /**
     * Get a quote by its ID
     */
    suspend fun getQuoteById(id: Int): QuoteEntity? = quoteDao.getQuoteById(id)

    /**
     * Get a random active quote, avoiding repetition
     * @param lastShownQuoteId ID of the last shown quote to avoid showing it again
     */
    suspend fun getRandomQuote(lastShownQuoteId: Int = -1): QuoteEntity? {
        val count = quoteDao.getActiveQuoteCount()
        
        return when {
            count == 0 -> null
            count == 1 -> quoteDao.getRandomActiveQuote()
            else -> {
                // Try to get a quote different from the last shown
                val quote = quoteDao.getRandomActiveQuoteExcluding(lastShownQuoteId)
                // Fallback if somehow exclusion didn't work
                quote ?: quoteDao.getRandomActiveQuote()
            }
        }
    }

    /**
     * Get count of active quotes
     */
    suspend fun getActiveQuoteCount(): Int = quoteDao.getActiveQuoteCount()

    /**
     * Insert a new quote
     * @return The ID of the newly inserted quote
     */
    suspend fun insertQuote(quote: QuoteEntity): Long = quoteDao.insertQuote(quote)

    /**
     * Update an existing quote
     */
    suspend fun updateQuote(quote: QuoteEntity) = quoteDao.updateQuote(quote)

    /**
     * Toggle the active status of a quote (soft delete/restore)
     */
    suspend fun toggleQuoteActive(id: Int, isActive: Boolean) = quoteDao.setQuoteActive(id, isActive)

    /**
     * Soft delete a quote (set isActive = false)
     */
    suspend fun softDeleteQuote(id: Int) = quoteDao.setQuoteActive(id, false)

    /**
     * Restore a soft-deleted quote
     */
    suspend fun restoreQuote(id: Int) = quoteDao.setQuoteActive(id, true)

    /**
     * Permanently delete a quote
     */
    suspend fun deleteQuote(quote: QuoteEntity) = quoteDao.deleteQuote(quote)

    /**
     * Permanently delete a quote by ID
     */
    suspend fun deleteQuoteById(id: Int) = quoteDao.deleteQuoteById(id)

    /**
     * Search quotes by text or author
     */
    fun searchQuotes(query: String): Flow<List<QuoteEntity>> = quoteDao.searchQuotes(query)
}
