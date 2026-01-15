package com.baverika.r_journal.repository

import com.baverika.r_journal.data.local.dao.CategoryWithCount
import com.baverika.r_journal.data.local.dao.TaskDao
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskCategory
import com.baverika.r_journal.data.local.entity.TaskPriority
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Repository for Task operations.
 * 
 * Acts as a single source of truth for task data, abstracting the data layer
 * from the rest of the application.
 */
class TaskRepository(private val taskDao: TaskDao) {
    
    // ==================== TASK OPERATIONS ====================
    
    /**
     * Get all tasks as a Flow.
     */
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()
    
    /**
     * Get all active (not completed) tasks.
     */
    val activeTasks: Flow<List<Task>> = taskDao.getActiveTasks()
    
    /**
     * Get all completed tasks.
     */
    val completedTasks: Flow<List<Task>> = taskDao.getCompletedTasks()
    
    /**
     * Get overdue tasks.
     */
    val overdueTasks: Flow<List<Task>> = taskDao.getOverdueTasks()
    
    /**
     * Get active task count.
     */
    val activeTaskCount: Flow<Int> = taskDao.getActiveTaskCount()
    
    /**
     * Get completed task count.
     */
    val completedTaskCount: Flow<Int> = taskDao.getCompletedTaskCount()
    
    /**
     * Get overdue task count.
     */
    val overdueTaskCount: Flow<Int> = taskDao.getOverdueTaskCount()
    
    /**
     * Get tasks by category.
     */
    fun getTasksByCategory(categoryId: String): Flow<List<Task>> {
        return taskDao.getTasksByCategory(categoryId)
    }
    
    /**
     * Get tasks by priority.
     */
    fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>> {
        return taskDao.getTasksByPriority(priority)
    }
    
    /**
     * Get tasks due today.
     */
    fun getTasksDueToday(): Flow<List<Task>> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return taskDao.getTasksDueToday(startOfDay, endOfDay)
    }
    
    /**
     * Get completed today count.
     */
    fun getCompletedTodayCount(): Flow<Int> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return taskDao.getCompletedTodayCount(startOfDay, endOfDay)
    }
    
    /**
     * Get upcoming tasks with limit.
     */
    fun getUpcomingTasks(limit: Int = 7): Flow<List<Task>> {
        return taskDao.getUpcomingTasks(System.currentTimeMillis(), limit)
    }
    
    /**
     * Search tasks by query.
     */
    fun searchTasks(query: String): Flow<List<Task>> {
        return taskDao.searchTasks(query)
    }
    
    /**
     * Get a task by ID.
     */
    suspend fun getTaskById(taskId: String): Task? {
        return taskDao.getTaskById(taskId)
    }
    
    /**
     * Insert a new task.
     */
    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }
    
    /**
     * Update an existing task.
     */
    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }
    
    /**
     * Delete a task.
     */
    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }
    
    /**
     * Delete a task by ID.
     */
    suspend fun deleteTaskById(taskId: String) {
        taskDao.deleteTaskById(taskId)
    }
    
    /**
     * Toggle task completion status.
     */
    suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean) {
        taskDao.updateTaskCompletion(taskId, isCompleted)
    }
    
    // ==================== SYNCHRONOUS METHODS FOR WIDGET ====================
    
    /**
     * Get upcoming tasks synchronously (for widget).
     */
    fun getUpcomingTasksSync(limit: Int = 7): List<Task> {
        return taskDao.getUpcomingTasksSync(limit)
    }
    
    /**
     * Toggle task completion synchronously (for widget).
     */
    fun toggleTaskCompletionSync(taskId: String, isCompleted: Boolean) {
        taskDao.updateTaskCompletionSync(taskId, isCompleted)
    }
    
    // ==================== CATEGORY OPERATIONS ====================
    
    /**
     * Get all categories.
     */
    val allCategories: Flow<List<TaskCategory>> = taskDao.getAllCategories()
    
    /**
     * Get categories with task count.
     */
    val categoriesWithCount: Flow<List<CategoryWithCount>> = taskDao.getCategoriesWithTaskCount()
    
    /**
     * Get all categories synchronously.
     */
    fun getAllCategoriesSync(): List<TaskCategory> {
        return taskDao.getAllCategoriesSync()
    }
    
    /**
     * Get a category by ID.
     */
    suspend fun getCategoryById(categoryId: String): TaskCategory? {
        return taskDao.getCategoryById(categoryId)
    }
    
    /**
     * Insert a new category.
     */
    suspend fun insertCategory(category: TaskCategory) {
        taskDao.insertCategory(category)
    }
    
    /**
     * Update an existing category.
     */
    suspend fun updateCategory(category: TaskCategory) {
        taskDao.updateCategory(category)
    }
    
    /**
     * Delete a category.
     */
    suspend fun deleteCategory(category: TaskCategory) {
        taskDao.deleteCategory(category)
    }
    
    /**
     * Create default categories if none exist.
     */
    suspend fun createDefaultCategoriesIfNeeded() {
        val categories = taskDao.getAllCategoriesSuspend()
        if (categories.isEmpty()) {
            val defaultCategories = listOf(
                TaskCategory(name = "Work", color = 0xFF2196F3.toInt(), icon = "work"),
                TaskCategory(name = "Personal", color = 0xFF4CAF50.toInt(), icon = "person"),
                TaskCategory(name = "Shopping", color = 0xFFFF9800.toInt(), icon = "shopping_cart"),
                TaskCategory(name = "Health", color = 0xFFE91E63.toInt(), icon = "favorite"),
                TaskCategory(name = "Finance", color = 0xFF9C27B0.toInt(), icon = "attach_money")
            )
            defaultCategories.forEach { insertCategory(it) }
        }
    }
}
