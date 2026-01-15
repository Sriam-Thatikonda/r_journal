package com.baverika.r_journal.data.local.dao

import androidx.room.*
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskCategory
import com.baverika.r_journal.data.local.entity.TaskPriority
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Task operations.
 * 
 * Provides CRUD operations and various queries for filtering and sorting tasks.
 */
@Dao
interface TaskDao {
    
    // ==================== TASK OPERATIONS ====================
    
    /**
     * Get all tasks ordered by due date (nulls last), then by priority.
     */
    @Query("""
        SELECT * FROM tasks 
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            CASE priority 
                WHEN 'HIGH' THEN 1 
                WHEN 'MEDIUM' THEN 2 
                WHEN 'LOW' THEN 3 
            END
    """)
    fun getAllTasks(): Flow<List<Task>>
    
    /**
     * Get all active (not completed) tasks.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            CASE priority 
                WHEN 'HIGH' THEN 1 
                WHEN 'MEDIUM' THEN 2 
                WHEN 'LOW' THEN 3 
            END
    """)
    fun getActiveTasks(): Flow<List<Task>>
    
    /**
     * Get all completed tasks.
     */
    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY updatedAt DESC")
    fun getCompletedTasks(): Flow<List<Task>>
    
    /**
     * Get tasks by category.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE categoryId = :categoryId 
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun getTasksByCategory(categoryId: String): Flow<List<Task>>
    
    /**
     * Get tasks by priority.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE priority = :priority 
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun getTasksByPriority(priority: TaskPriority): Flow<List<Task>>
    
    /**
     * Get overdue tasks (due date passed and not completed).
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE dueDate < :currentTime AND isCompleted = 0 
        ORDER BY dueDate ASC
    """)
    fun getOverdueTasks(currentTime: Long = System.currentTimeMillis()): Flow<List<Task>>
    
    /**
     * Get tasks due today.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE dueDate >= :startOfDay AND dueDate < :endOfDay 
        ORDER BY dueDate ASC, 
            CASE priority 
                WHEN 'HIGH' THEN 1 
                WHEN 'MEDIUM' THEN 2 
                WHEN 'LOW' THEN 3 
            END
    """)
    fun getTasksDueToday(startOfDay: Long, endOfDay: Long): Flow<List<Task>>
    
    /**
     * Get upcoming tasks (next N tasks by due date).
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 AND dueDate >= :currentTime 
        ORDER BY dueDate ASC 
        LIMIT :limit
    """)
    fun getUpcomingTasks(currentTime: Long, limit: Int = 7): Flow<List<Task>>
    
    /**
     * Search tasks by title or description.
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC
    """)
    fun searchTasks(query: String): Flow<List<Task>>
    
    /**
     * Get a single task by ID.
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?
    
    /**
     * Get task count statistics.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getActiveTaskCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1")
    fun getCompletedTaskCount(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE dueDate < :currentTime AND isCompleted = 0")
    fun getOverdueTaskCount(currentTime: Long = System.currentTimeMillis()): Flow<Int>
    
    @Query("""
        SELECT COUNT(*) FROM tasks 
        WHERE dueDate >= :startOfDay AND dueDate < :endOfDay AND isCompleted = 1
    """)
    fun getCompletedTodayCount(startOfDay: Long, endOfDay: Long): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    // ==================== SYNCHRONOUS METHODS FOR WIDGET ====================
    
    /**
     * Get upcoming tasks synchronously (for widget).
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            CASE priority 
                WHEN 'HIGH' THEN 1 
                WHEN 'MEDIUM' THEN 2 
                WHEN 'LOW' THEN 3 
            END
        LIMIT :limit
    """)
    fun getUpcomingTasksSync(limit: Int = 7): List<Task>
    
    @Query("UPDATE tasks SET isCompleted = :isCompleted, updatedAt = :updatedAt WHERE id = :taskId")
    fun updateTaskCompletionSync(taskId: String, isCompleted: Boolean, updatedAt: Long = System.currentTimeMillis())
    
    // ==================== CATEGORY OPERATIONS ====================
    
    @Query("SELECT * FROM task_categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<TaskCategory>>
    
    @Query("SELECT * FROM task_categories ORDER BY name ASC")
    suspend fun getAllCategoriesSuspend(): List<TaskCategory>
    
    @Query("SELECT * FROM task_categories ORDER BY name ASC")
    fun getAllCategoriesSync(): List<TaskCategory>
    
    @Query("SELECT * FROM task_categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: String): TaskCategory?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TaskCategory)
    
    @Update
    suspend fun updateCategory(category: TaskCategory)
    
    @Delete
    suspend fun deleteCategory(category: TaskCategory)
    
    /**
     * Get task count per category.
     */
    @Query("""
        SELECT tc.*, COUNT(t.id) as taskCount 
        FROM task_categories tc 
        LEFT JOIN tasks t ON tc.id = t.categoryId AND t.isCompleted = 0
        GROUP BY tc.id
        ORDER BY tc.name ASC
    """)
    fun getCategoriesWithTaskCount(): Flow<List<CategoryWithCount>>
}

/**
 * Data class for category with task count.
 */
data class CategoryWithCount(
    val id: String,
    val name: String,
    val color: Int,
    val icon: String,
    val createdAt: Long,
    val taskCount: Int
)
