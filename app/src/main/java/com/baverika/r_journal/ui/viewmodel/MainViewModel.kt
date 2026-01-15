package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _showBirthdayEasterEgg = MutableStateFlow(false)
    val showBirthdayEasterEgg = _showBirthdayEasterEgg.asStateFlow()

    private val _userAge = MutableStateFlow(0)
    val userAge = _userAge.asStateFlow()

    init {
        checkBirthday()
    }

    fun checkBirthday() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!settingsRepository.specialMomentsEnabled) return@launch

            val today = LocalDate.now()
            val birthDay = settingsRepository.birthDay
            val birthMonth = settingsRepository.birthMonth
            val birthYear = settingsRepository.birthYear
            val lastShownYear = settingsRepository.lastBirthdayShownYear

            if (today.dayOfMonth == birthDay &&
                today.monthValue == birthMonth &&
                today.year != lastShownYear
            ) {
                _userAge.value = today.year - birthYear
                _showBirthdayEasterEgg.value = true
            }
        }
    }

    fun markBirthdayShown() {
        val today = LocalDate.now()
        settingsRepository.lastBirthdayShownYear = today.year
        _showBirthdayEasterEgg.value = false
    }
}

class MainViewModelFactory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
