package com.baverika.r_journal.data.local.dao

import androidx.room.*
import com.baverika.r_journal.data.local.entity.QuickNote
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickNoteDao {

    @Query("SELECT * FROM quick_notes ORDER BY timestamp DESC")
    fun getAllNotes(): Flow<List<QuickNote>>

    @Query("SELECT * FROM quick_notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchNotes(query: String): Flow<List<QuickNote>>

    @Query("SELECT * FROM quick_notes WHERE id = :id")
    suspend fun getNoteById(id: String): QuickNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: QuickNote)

    @Update
    suspend fun updateNote(note: QuickNote)

    @Delete
    suspend fun deleteNote(note: QuickNote)
}