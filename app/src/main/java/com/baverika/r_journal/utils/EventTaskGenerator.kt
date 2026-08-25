package com.baverika.r_journal.utils

import android.content.Context
import com.baverika.r_journal.data.local.entity.Event
import com.baverika.r_journal.data.local.entity.EventType
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskPriority
import com.baverika.r_journal.repository.TaskRepository
import com.baverika.r_journal.tasks.widget.TaskWidgetProvider
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object EventTaskGenerator {

    /**
     * Checks events against current date and tomorrow's date.
     * Auto-creates tasks 1 day in advance for upcoming events without emojis.
     */
    suspend fun ensureEventTasksCreated(
        events: List<Event>,
        taskRepository: TaskRepository,
        context: Context? = null
    ) {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)

        val targetDates = listOf(today, tomorrow)

        for (targetDate in targetDates) {
            val matchingEvents = events.filter { event ->
                val isDirectMatch = (event.day == targetDate.dayOfMonth && event.month == targetDate.monthValue)
                val isLeapMatch = (event.month == 2 && event.day == 29 && !targetDate.isLeapYear && targetDate.monthValue == 2 && targetDate.dayOfMonth == 28)
                isDirectMatch || isLeapMatch
            }

            for (event in matchingEvents) {
                val cleanTitle = formatEventTaskTitle(event)
                val targetMillis = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val deterministicId = "event_task_${event.id}_${targetDate.year}_${targetDate.monthValue}_${targetDate.dayOfMonth}"

                // Check if task already exists
                val existingTask = taskRepository.getTaskById(deterministicId)
                if (existingTask == null) {
                    val newTask = Task(
                        id = deterministicId,
                        title = cleanTitle,
                        description = "Event on ${targetDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}",
                        dueDate = targetMillis,
                        priority = TaskPriority.HIGH,
                        isCompleted = false
                    )
                    taskRepository.insertTask(newTask)
                    context?.let { TaskWidgetProvider.requestUpdate(it) }
                }
            }
        }
    }

    /**
     * Cleanly formats task title for an event without any emojis.
     */
    private fun formatEventTaskTitle(event: Event): String {
        val rawTitle = event.title.trim()
        return when (event.type) {
            EventType.BIRTHDAY -> {
                if (rawTitle.contains("birthday", ignoreCase = true)) rawTitle else "$rawTitle's Birthday"
            }
            EventType.ANNIVERSARY -> {
                if (rawTitle.contains("anniversary", ignoreCase = true) || rawTitle.contains("marriage", ignoreCase = true)) {
                    rawTitle
                } else {
                    "$rawTitle's Anniversary"
                }
            }
            EventType.MEETING -> {
                if (rawTitle.contains("meeting", ignoreCase = true)) rawTitle else "$rawTitle Meeting"
            }
            EventType.CUSTOM -> rawTitle
        }
    }
}
