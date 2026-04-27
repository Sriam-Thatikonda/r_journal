package com.baverika.r_journal.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.CravingLogEntity
import com.baverika.r_journal.repository.CravingQuestRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class CravingQuestViewModel(private val repository: CravingQuestRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<List<CravingLogEntity>>(emptyList())
    val uiState: StateFlow<List<CravingLogEntity>> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getAllLogs().collect {
                _uiState.value = it
            }
        }
    }

    fun isJunkFood(food: String): Boolean = repository.isJunkFood(food)

    fun addCraving(food: String, location: String, onQuestGenerated: (CravingLogEntity) -> Unit) {
        viewModelScope.launch {
            val (difficulty, questText) = repository.generateQuest(location)
            val newLog = CravingLogEntity(
                food = food,
                location = location,
                quest = questText,
                difficulty = difficulty
            )
            repository.insertLog(newLog)
            onQuestGenerated(newLog)
        }
    }

    fun toggleQuestCompleted(log: CravingLogEntity) {
        if (isReadOnly(log)) return
        viewModelScope.launch {
            val updated = log.copy(
                questCompleted = !log.questCompleted,
                questCompletedAt = if (!log.questCompleted) System.currentTimeMillis() else null
            )
            repository.updateLog(updated)
        }
    }

    fun toggleFoodEaten(log: CravingLogEntity) {
        if (isReadOnly(log)) return
        viewModelScope.launch {
            val updated = log.copy(
                foodEaten = !log.foodEaten,
                foodEatenAt = if (!log.foodEaten) System.currentTimeMillis() else null
            )
            repository.updateLog(updated)
        }
    }

    fun isReadOnly(log: CravingLogEntity): Boolean {
        val entryDate = Instant.ofEpochMilli(log.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        return entryDate.isBefore(today)
    }

    suspend fun getTodayStats(): Pair<Int, String> {
        val count = repository.getTodayCount()
        val level = when {
            count == 0 -> "Easy"
            count == 1 -> "Medium"
            count == 2 -> "Hard"
            else -> "Boss"
        }
        return count to level
    }
}

class CravingQuestViewModelFactory(private val repository: CravingQuestRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CravingQuestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CravingQuestViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
