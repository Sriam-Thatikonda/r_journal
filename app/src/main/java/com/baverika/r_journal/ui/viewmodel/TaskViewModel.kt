package com.baverika.r_journal.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.dao.CategoryWithCount
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskCategory
import com.baverika.r_journal.data.local.entity.TaskPriority
import com.baverika.r_journal.repository.TaskRepository
import com.baverika.r_journal.tasks.widget.TaskWidgetProvider
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Filter options for task list.
 */
enum class TaskFilter {
    ALL,
    ACTIVE,
    COMPLETED,
    OVERDUE,
    TODAY
}

/**
 * Sort options for task list.
 */
enum class TaskSortOption {
    DUE_DATE,
    PRIORITY,
    CREATED_DATE,
    ALPHABETICAL
}

/**
 * UI state for task statistics.
 */
data class TaskStats(
    val totalTasks: Int = 0,
    val activeTasks: Int = 0,
    val completedTasks: Int = 0,
    val overdueTasks: Int = 0,
    val completedToday: Int = 0
)

/**
 * ViewModel for Task management.
 * 
 * Handles all business logic for task operations, filtering, sorting,
 * and category management.
 */
class TaskViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {
    
    // ==================== FILTER & SORT STATE ====================
    
    private val _currentFilter = MutableStateFlow(TaskFilter.ALL)
    val currentFilter: StateFlow<TaskFilter> = _currentFilter.asStateFlow()
    
    private val _currentSort = MutableStateFlow(TaskSortOption.DUE_DATE)
    val currentSort: StateFlow<TaskSortOption> = _currentSort.asStateFlow()
    
    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()
    
    private val _selectedPriority = MutableStateFlow<TaskPriority?>(null)
    val selectedPriority: StateFlow<TaskPriority?> = _selectedPriority.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // ==================== LOADING & ERROR STATE ====================
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ==================== UI EVENTS ====================
    private val _uiEvent = MutableSharedFlow<TaskUiEvent>()
    val uiEvent: SharedFlow<TaskUiEvent> = _uiEvent.asSharedFlow()

    sealed class TaskUiEvent {
        data object NavigateBack : TaskUiEvent()
    }
    
    // ==================== DATA FLOWS ====================
    
    /**
     * Categories list.
     */
    val categories: StateFlow<List<TaskCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Categories with task count.
     */
    val categoriesWithCount: StateFlow<List<CategoryWithCount>> = repository.categoriesWithCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Filtered and sorted tasks based on current filter, sort, and search query.
     */
    val tasks: StateFlow<List<Task>> = combine(
        _currentFilter,
        _selectedCategoryId,
        _selectedPriority,
        _searchQuery
    ) { filter, categoryId, priority, query ->
        FilterParams(filter, categoryId, priority, query)
    }.flatMapLatest { params ->
        when {
            params.query.isNotBlank() -> repository.searchTasks(params.query)
            params.categoryId != null -> repository.getTasksByCategory(params.categoryId)
            params.priority != null -> repository.getTasksByPriority(params.priority)
            else -> when (params.filter) {
                TaskFilter.ALL -> repository.allTasks
                TaskFilter.ACTIVE -> repository.activeTasks
                TaskFilter.COMPLETED -> repository.completedTasks
                TaskFilter.OVERDUE -> repository.overdueTasks
                TaskFilter.TODAY -> repository.getTasksDueToday()
            }
        }
    }.combine(_currentSort) { tasks, sortOption ->
        sortTasks(tasks, sortOption)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    /**
     * Task statistics.
     */
    val taskStats: StateFlow<TaskStats> = combine(
        repository.activeTaskCount,
        repository.completedTaskCount,
        repository.overdueTaskCount,
        repository.getCompletedTodayCount()
    ) { active, completed, overdue, completedToday ->
        TaskStats(
            totalTasks = active + completed,
            activeTasks = active,
            completedTasks = completed,
            overdueTasks = overdue,
            completedToday = completedToday
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TaskStats())
    
    init {
        // Create default categories on first run
        viewModelScope.launch {
            repository.createDefaultCategoriesIfNeeded()
        }
    }
    
    // ==================== FILTER & SORT ACTIONS ====================
    
    fun setFilter(filter: TaskFilter) {
        _currentFilter.value = filter
        // Clear category and priority filters when changing main filter
        _selectedCategoryId.value = null
        _selectedPriority.value = null
    }
    
    fun setSort(sortOption: TaskSortOption) {
        _currentSort.value = sortOption
    }
    
    fun filterByCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
        if (categoryId != null) {
            _currentFilter.value = TaskFilter.ALL
            _selectedPriority.value = null
        }
    }
    
    fun filterByPriority(priority: TaskPriority?) {
        _selectedPriority.value = priority
        if (priority != null) {
            _currentFilter.value = TaskFilter.ALL
            _selectedCategoryId.value = null
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun clearFilters() {
        _currentFilter.value = TaskFilter.ALL
        _selectedCategoryId.value = null
        _selectedPriority.value = null
        _searchQuery.value = ""
    }
    
    // ==================== TASK OPERATIONS ====================
    
    /**
     * Get a task by ID.
     */
    suspend fun getTaskById(taskId: String): Task? {
        return repository.getTaskById(taskId)
    }
    
    /**
     * Add a new task.
     */
    fun addTask(
        title: String,
        description: String = "",
        dueDate: Long? = null,
        priority: TaskPriority = TaskPriority.MEDIUM,
        categoryId: String? = null,
        reminderTime: Long? = null
    ) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    _isLoading.value = true
                    val task = Task(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        priority = priority,
                        categoryId = categoryId,
                        reminderTime = reminderTime
                    )
                    repository.insertTask(task)
                    updateWidget()
                    
                    // Schedule notification if reminder is set
                    if (reminderTime != null) {
                        scheduleTaskReminder(task)
                    }
                    _uiEvent.emit(TaskUiEvent.NavigateBack)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to add task: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Update an existing task.
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    _isLoading.value = true
                    repository.updateTask(task)
                    updateWidget()
                    
                    // Update reminder if changed
                    if (task.reminderTime != null) {
                        scheduleTaskReminder(task)
                    } else {
                        cancelTaskReminder(task.id)
                    }
                    _uiEvent.emit(TaskUiEvent.NavigateBack)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to update task: ${e.message}"
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }
    
    /**
     * Delete a task.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    repository.deleteTask(task)
                    cancelTaskReminder(task.id)
                    updateWidget()
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to delete task: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Delete a task by ID.
     */
    fun deleteTaskById(taskId: String) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    repository.deleteTaskById(taskId)
                    cancelTaskReminder(taskId)
                    updateWidget()
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to delete task: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Toggle task completion status.
     */
    fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    repository.toggleTaskCompletion(taskId, isCompleted)
                    updateWidget()
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to update task: ${e.message}"
                }
            }
        }
    }
    
    // ==================== CATEGORY OPERATIONS ====================
    
    /**
     * Get a category by ID.
     */
    suspend fun getCategoryById(categoryId: String): TaskCategory? {
        return repository.getCategoryById(categoryId)
    }
    
    /**
     * Add a new category.
     */
    fun addCategory(name: String, color: Int, icon: String = "folder") {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    val category = TaskCategory(
                        name = name,
                        color = color,
                        icon = icon
                    )
                    repository.insertCategory(category)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to add category: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Update an existing category.
     */
    fun updateCategory(category: TaskCategory) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    repository.updateCategory(category)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to update category: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Delete a category.
     */
    fun deleteCategory(category: TaskCategory) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    repository.deleteCategory(category)
                } catch (e: Exception) {
                    _errorMessage.value = "Failed to delete category: ${e.message}"
                }
            }
        }
    }
    
    // ==================== HELPER METHODS ====================
    
    private fun sortTasks(tasks: List<Task>, sortOption: TaskSortOption): List<Task> {
        return when (sortOption) {
            TaskSortOption.DUE_DATE -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.dueDate == null }
                    .thenBy { it.dueDate }
            )
            TaskSortOption.PRIORITY -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { 
                        when (it.priority) {
                            TaskPriority.HIGH -> 0
                            TaskPriority.MEDIUM -> 1
                            TaskPriority.LOW -> 2
                        }
                    }
            )
            TaskSortOption.CREATED_DATE -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenByDescending { it.createdAt }
            )
            TaskSortOption.ALPHABETICAL -> tasks.sortedWith(
                compareBy<Task> { it.isCompleted }
                    .thenBy { it.title.lowercase() }
            )
        }
    }
    
    private fun updateWidget() {
        val context = getApplication<Application>()
        try {
            val intent = Intent(context, TaskWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                ComponentName(context, TaskWidgetProvider::class.java)
            )
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            // Widget might not be on home screen, ignore
        }
    }
    
    private fun scheduleTaskReminder(task: Task) {
        // TODO: Implement using WorkManager
        // This will be implemented in the notification worker
    }
    
    private fun cancelTaskReminder(taskId: String) {
        // TODO: Cancel scheduled notification
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    private data class FilterParams(
        val filter: TaskFilter,
        val categoryId: String?,
        val priority: TaskPriority?,
        val query: String
    )
}
