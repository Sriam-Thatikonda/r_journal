package com.baverika.r_journal.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to create DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "quick_notes_preferences")

class QuickNotesPreferences(private val context: Context) {
    
    companion object {
        private val LAYOUT_TYPE_KEY = stringPreferencesKey("layout_type")
        const val LAYOUT_LIST = "list"
        const val LAYOUT_MASONRY = "masonry"
    }
    
    // Flow to observe layout type changes
    val layoutType: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LAYOUT_TYPE_KEY] ?: LAYOUT_MASONRY // Default to masonry
    }
    
    // Function to save layout type
    suspend fun saveLayoutType(layoutType: String) {
        context.dataStore.edit { preferences ->
            preferences[LAYOUT_TYPE_KEY] = layoutType
        }
    }
}
