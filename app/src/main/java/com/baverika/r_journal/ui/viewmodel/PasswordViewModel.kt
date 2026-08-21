package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.Password
import com.baverika.r_journal.repository.PasswordRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOrder {
    NEWEST_FIRST,
    NAME_A_Z,
    NAME_Z_A,
    TYPE_PIN_FIRST
}

class PasswordViewModel(private val repository: PasswordRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NAME_A_Z)
    val sortOrder = _sortOrder.asStateFlow()

    // Combined flow: search + sort applied in-memory on top of Room's full list
    val passwords: StateFlow<List<Password>> = _searchQuery
        .combine(repository.allPasswords) { query, allPasswords ->
            if (query.isBlank()) allPasswords
            else allPasswords.filter {
                it.siteName.contains(query, ignoreCase = true) ||
                        it.username.contains(query, ignoreCase = true)
            }
        }
        .combine(_sortOrder) { filtered, order ->
            when (order) {
                SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.createdAt }
                SortOrder.NAME_A_Z     -> filtered.sortedBy { it.siteName.lowercase() }
                SortOrder.NAME_Z_A     -> filtered.sortedByDescending { it.siteName.lowercase() }
                SortOrder.TYPE_PIN_FIRST -> filtered.sortedWith(
                    compareByDescending<Password> {
                        it.type.name == "PIN"
                    }.thenBy { it.siteName.lowercase() }
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChanged(order: SortOrder) {
        _sortOrder.value = order
    }

    fun addPassword(
        siteName: String,
        username: String,
        passwordValue: String,
        type: com.baverika.r_journal.data.local.entity.PasswordType =
            com.baverika.r_journal.data.local.entity.PasswordType.PASSWORD
    ) {
        viewModelScope.launch {
            repository.insertPassword(
                Password(
                    siteName = siteName,
                    username = username,
                    passwordValue = passwordValue,
                    type = type
                )
            )
        }
    }

    fun updatePassword(password: Password) {
        viewModelScope.launch {
            repository.updatePassword(password)
        }
    }

    fun deletePassword(password: Password) {
        viewModelScope.launch {
            repository.deletePassword(password)
        }
    }
}

class PasswordViewModelFactory(private val repository: PasswordRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PasswordViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
