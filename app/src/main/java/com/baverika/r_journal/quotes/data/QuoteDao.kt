package com.baverika.r_journal.quotes.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Quote operations.
 * Provides both Flow-based reactive queries and suspend functions for one-shot operations.
 */
@Dao
interface QuoteDao {

    /**
     * Get all quotes ordered by creation date (newest first).
     * Returns Flow for reactive updates.
     */
    @Query("SELECT * FROM quotes ORDER BY createdAt DESC")
    fun getAllQuotes(): Flow<List<QuoteEntity>>

    /**
     * Get only active quotes for widget display.
     * Returns Flow for reactive updates.
     */
    @Query("SELECT * FROM quotes WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveQuotes(): Flow<List<QuoteEntity>>

    /**
     * Get active quotes synchronously (for widget updates from background)
     */
    @Query("SELECT * FROM quotes WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun getActiveQuotesSync(): List<QuoteEntity>

    /**
     * Get a single quote by ID
     */
    @Query("SELECT * FROM quotes WHERE id = :id")
    suspend fun getQuoteById(id: Int): QuoteEntity?

    /**
     * Get a random active quote, excluding a specific ID to avoid repetition
     */
    @Query("SELECT * FROM quotes WHERE isActive = 1 AND id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomActiveQuoteExcluding(excludeId: Int): QuoteEntity?

    /**
     * Get a random active quote (when no exclusion needed)
     */
    @Query("SELECT * FROM quotes WHERE isActive = 1 ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomActiveQuote(): QuoteEntity?

    /**
     * Get count of active quotes
     */
    @Query("SELECT COUNT(*) FROM quotes WHERE isActive = 1")
    suspend fun getActiveQuoteCount(): Int

    /**
     * Insert a new quote
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: QuoteEntity): Long

    /**
     * Update an existing quote
     */
    @Update
    suspend fun updateQuote(quote: QuoteEntity)

    /**
     * Toggle the active status of a quote (soft delete/restore)
     */
    @Query("UPDATE quotes SET isActive = :isActive WHERE id = :id")
    suspend fun setQuoteActive(id: Int, isActive: Boolean)

    /**
     * Hard delete a quote (permanent removal)
     */
    @Delete
    suspend fun deleteQuote(quote: QuoteEntity)

    /**
     * Delete quote by ID
     */
    @Query("DELETE FROM quotes WHERE id = :id")
    suspend fun deleteQuoteById(id: Int)

    /**
     * Search quotes by text content
     */
    @Query("SELECT * FROM quotes WHERE text LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchQuotes(query: String): Flow<List<QuoteEntity>>
}
