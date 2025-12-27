// app/src/main/java/com/baverika/r_journal/repository/QuickNoteRepository.kt

package com.baverika.r_journal.repository

import com.baverika.r_journal.data.local.dao.QuickNoteDao
import com.baverika.r_journal.data.local.entity.QuickNote
import kotlinx.coroutines.flow.Flow

class QuickNoteRepository(private val dao: QuickNoteDao) {

    val allNotes: Flow<List<QuickNote>> = dao.getAllNotes()

    fun searchNotes(query: String): Flow<List<QuickNote>> = dao.searchNotes(query)

    suspend fun insertNote(note: QuickNote) = dao.insertNote(note)

    suspend fun updateNote(note: QuickNote) = dao.updateNote(note)

    suspend fun deleteNote(note: QuickNote) = dao.deleteNote(note)

    suspend fun upsertNote(note: QuickNote) {
        // This relies on your QuickNoteDao's insertNote method using
        // @Insert(onConflict = OnConflictStrategy.REPLACE).
        // REPLACE means if a note with the same @PrimaryKey (id) exists, it will be replaced.
        dao.insertNote(note)
    }
}