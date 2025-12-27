package com.baverika.r_journal.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.Habit
import com.baverika.r_journal.data.local.entity.HabitLog
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.widget.HabitWidgetProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

class HabitViewModel(
    application: Application,
    private val repository: JournalRepository
) : AndroidViewModel(application) {

    // Selected date for viewing habits (default: Today)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // All active habits
    private val _habits = repository.allActiveHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Logs for the selected date
    private val _habitLogs = _selectedDate.flatMapLatest { date ->
        val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        repository.getHabitLogsForDate(dateMillis)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Combined state: List of (Habit, isCompleted)
    val habitState = combine(_habits, _habitLogs, _selectedDate) { habits, logs, date ->
        habits.filter { habit ->
            // Filter by frequency (day of week)
            // 1=Mon, 7=Sun. LocalDate.dayOfWeek.value is 1=Mon, 7=Sun.
            val dayOfWeek = date.dayOfWeek.value
            habit.frequency.contains(dayOfWeek)
        }.map { habit ->
            val isCompleted = logs.any { it.habitId == habit.id && it.isCompleted }
            HabitUiState(habit, isCompleted)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly Stats (Last 7 days)
    val weeklyStats = flow {
        val end = LocalDate.now()
        val start = end.minusDays(6) // Last 7 days
        
        val startMillis = start.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val endMillis = end.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toEpochSecond() * 1000
        
        emitAll(
            repository.getHabitLogsBetween(startMillis, endMillis).map { logs ->
                (0..6).map { i ->
                    val date = start.plusDays(i.toLong())
                    val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
                    val count = logs.count { 
                        it.dateMillis == dateMillis && it.isCompleted 
                    }
                    date to count
                }
            }
        )
    }.stateIn(
        viewModelScope, 
        SharingStarted.WhileSubscribed(5000), 
        (0..6).map { LocalDate.now().minusDays(6 - it.toLong()) to 0 }
    )

    fun updateSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private fun updateWidget() {
        val context = getApplication<Application>()
        val intent = Intent(context, HabitWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, HabitWidgetProvider::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    fun toggleHabit(habitId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val dateMillis = _selectedDate.value.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
                repository.toggleHabitCompletion(habitId, dateMillis, isCompleted)
                updateWidget()
            }
        }
    }

    fun addHabit(title: String, description: String, frequency: List<Int>, color: Int) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                // Get next available color if color is default
                val finalColor = if (color == 0xFFFF6B9D.toInt()) {
                    getNextAvailableColor()
                } else {
                    color
                }
                
                val habit = Habit(
                    title = title,
                    description = description,
                    frequency = frequency,
                    color = finalColor
                )
                repository.addHabit(habit)
                updateWidget()
            }
        }
    }
    
    private suspend fun getNextAvailableColor(): Int {
        val colorPalette = listOf(
            0xFFFF6B9D.toInt(), // Vibrant Pink
            0xFF4ECDC4.toInt(), // Turquoise
            0xFFFFA07A.toInt(), // Light Salmon
            0xFF98D8C8.toInt(), // Mint Green
            0xFF6C5CE7.toInt(), // Soft Purple
            0xFFFD79A8.toInt(), // Hot Pink
            0xFF00D2FF.toInt(), // Bright Cyan
            0xFFFFBE76.toInt(), // Peach
            0xFFFF7979.toInt(), // Coral Red
            0xFF95E1D3.toInt()  // Aqua Green
        )
        
        // Get all existing habits
        val existingHabits = _habits.value
        val usedColors = existingHabits.map { it.color }.toSet()
        
        // Find first unused color
        val availableColor = colorPalette.firstOrNull { !usedColors.contains(it) }
        
        // If all colors used, cycle through palette based on habit count
        return availableColor ?: colorPalette[existingHabits.size % colorPalette.size]
    }

    suspend fun getHabitById(id: String): Habit? {
        return repository.getHabitById(id)
    }

    fun updateHabit(id: String, title: String, description: String, frequency: List<Int>, color: Int) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val existingHabit = getHabitById(id)
                if (existingHabit != null) {
                    val updatedHabit = existingHabit.copy(
                        title = title,
                        description = description,
                        frequency = frequency,
                        color = color
                    )
                    repository.updateHabit(updatedHabit)
                    updateWidget()
                }
            }
        }
    }

    fun updateHabit(habit: Habit) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repository.updateHabit(habit)
                updateWidget()
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                repository.deleteHabit(habit)
                updateWidget()
            }
        }
    }

    fun deleteHabitById(id: String) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val habit = getHabitById(id)
                if (habit != null) {
                    repository.deleteHabit(habit)
                    updateWidget()
                }
            }
        }
    }
}

data class HabitUiState(
    val habit: Habit,
    val isCompleted: Boolean
)
