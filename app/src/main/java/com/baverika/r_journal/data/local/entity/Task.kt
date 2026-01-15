package com.baverika.r_journal.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Priority levels for tasks with associated colors.
 */
enum class TaskPriority(val displayName: String, val colorValue: Long) {
    HIGH("High", 0xFFE53935),      // Red
    MEDIUM("Medium", 0xFFFFA726),  // Orange
    LOW("Low", 0xFF66BB6A);        // Green
    
    companion object {
        fun fromString(value: String): TaskPriority {
            return entries.find { it.name == value } ?: MEDIUM
        }
    }
}

/**
 * Room entity representing a Task item.
 * 
 * This entity stores all task-related information including title, description,
 * due date, priority, category, and completion status.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class Task(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val dueDate: Long? = null,           // Due date/time in millis (null = no due date)
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val categoryId: String? = null,       // Foreign key to TaskCategory
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reminderTime: Long? = null,       // Optional reminder time in millis
    val isRecurring: Boolean = false,     // For recurring tasks
    val recurringPattern: String? = null  // E.g., "daily", "weekly", "monthly"
)

/**
 * Room entity representing a Task Category.
 * 
 * Users can create custom categories to organize their tasks.
 */
@Entity(tableName = "task_categories")
data class TaskCategory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: Int = 0xFF2196F3.toInt(), // Default blue color
    val icon: String = "folder",          // Icon name for the category
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Data class for Task with Category information (for JOIN queries).
 */
data class TaskWithCategory(
    val task: Task,
    val category: TaskCategory?
)
