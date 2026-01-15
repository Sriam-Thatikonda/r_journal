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
import kotlinx.coroutines.Dispatchers
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

    // Yearly Logic
    fun getHabitLogsForYear(habitId: String, year: Int): Flow<List<HabitLog>> {
        val startOfYear = LocalDate.of(year, 1, 1)
        val endOfYear = LocalDate.of(year, 12, 31)
        val startMillis = startOfYear.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val endMillis = endOfYear.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toEpochSecond() * 1000
        
        return repository.getHabitLogsBetween(startMillis, endMillis).map { logs ->
            logs.filter { it.habitId == habitId }
        }
    }
    
    fun toggleHabitForDate(habitId: String, date: LocalDate, isDone: Boolean) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
                repository.toggleHabitCompletion(habitId, dateMillis, isDone)
                updateWidget()
            }
        }
    }
    
    // Yearly Dashboard Data
    data class YearlyHabitGrid(
        val habit: Habit,
        val days: List<DayState>
    )

    data class DayState(
        val date: LocalDate,
        val status: HabitStatus
    )
    
    enum class HabitStatus {
        DISABLED, PENDING, DONE
    }

    val yearlyHabitGrids: StateFlow<List<YearlyHabitGrid>> = flow {
        val today = LocalDate.now()
        val year = today.year
        val startOfYear = LocalDate.of(year, 1, 1)
        val endOfYear = LocalDate.of(year, 12, 31)

        val startMillis = startOfYear.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val endMillis = endOfYear.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toEpochSecond() * 1000

        // Precompute days and their millis to avoid repeated calculations in loops
        val daysInYearWithMillis = (0 until startOfYear.lengthOfYear()).map { offset ->
            val date = startOfYear.plusDays(offset.toLong())
            val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
            date to dateMillis
        }
       
        combine(
             repository.allActiveHabits,
             repository.getHabitLogsBetween(startMillis, endMillis)
        ) { habits, logs ->
            // Optimization: Create a lookup map for completed logs
            // Map<HabitId, Set<DateMillis>> for O(1) access
            val completedLogsMap = logs
                .filter { it.isCompleted }
                .groupBy { it.habitId }
                .mapValues { entry ->
                    entry.value.map { it.dateMillis }.toSet()
                }

            habits.map { habit ->
                val habitStartDate = java.time.Instant.ofEpochMilli(habit.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val dayStates = daysInYearWithMillis.map { (date, dateMillis) ->
                    val status = when {
                        !habit.frequency.contains(date.dayOfWeek.value) -> HabitStatus.DISABLED
                        date.isBefore(habitStartDate) -> HabitStatus.DISABLED 
                        completedLogsMap[habit.id]?.contains(dateMillis) == true -> HabitStatus.DONE
                        else -> HabitStatus.PENDING
                    }
                    DayState(date, status)
                }
                YearlyHabitGrid(habit, dayStates)
            }
        }.collect { emit(it) }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Data (Today + 6 days)
    val dashboardHabitGrids: StateFlow<List<YearlyHabitGrid>> = flow {
        val today = LocalDate.now()
        // Today + 6 days = 7 days total
        val days = (0..6).map { today.plusDays(it.toLong()) }
        
        val startMillis = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
        val end = today.plusDays(6)
        val endMillis = end.atStartOfDay(ZoneId.systemDefault()).plusDays(1).minusNanos(1).toEpochSecond() * 1000

        val daysWithMillis = days.map { date ->
             val dateMillis = date.atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
             date to dateMillis
        }

        combine(
             repository.allActiveHabits,
             repository.getHabitLogsBetween(startMillis, endMillis)
        ) { habits, logs ->
             val completedLogsMap = logs
                .filter { it.isCompleted }
                .groupBy { it.habitId }
                .mapValues { entry ->
                    entry.value.map { it.dateMillis }.toSet()
                }

             habits.map { habit ->
                val habitStartDate = java.time.Instant.ofEpochMilli(habit.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                val dayStates = daysWithMillis.map { (date, dateMillis) ->
                    val status = when {
                        !habit.frequency.contains(date.dayOfWeek.value) -> HabitStatus.DISABLED
                        date.isBefore(habitStartDate) -> HabitStatus.DISABLED
                        completedLogsMap[habit.id]?.contains(dateMillis) == true -> HabitStatus.DONE
                        else -> HabitStatus.PENDING
                    }
                    DayState(date, status)
                }
                YearlyHabitGrid(habit, dayStates)
            }
        }.collect { emit(it) }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Restored methods
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
        
        val existingHabits = _habits.value
        val usedColors = existingHabits.map { it.color }.toSet()
        val availableColor = colorPalette.firstOrNull { !usedColors.contains(it) }
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


