package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.Habit
import com.baverika.r_journal.ui.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    viewModel: HabitViewModel,
    navController: NavController,
    habitId: String,
    initialMonth: Int
) {
    // Fetch habit details
    var habit by remember { mutableStateOf<Habit?>(null) }
    
    LaunchedEffect(habitId) {
        habit = viewModel.getHabitById(habitId)
    }

    val currentYear = LocalDate.now().year
    
    // We need logs for the whole year. 
    val logs by viewModel.getHabitLogsForYear(habitId, currentYear).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(habit?.title ?: "Habit Detail") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (habit == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
             // Show only the selected month
             val yearMonth = YearMonth.of(currentYear, initialMonth)
             
             Column(
                 modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                 MonthDetailGrid(
                     habit = habit!!,
                     month = yearMonth,
                     logs = logs,
                     onToggleDate = { date, isCompleted ->
                        viewModel.toggleHabitForDate(habitId, date, isCompleted)
                     }
                 )
             }
        }
    }
}

@Composable
fun MonthDetailGrid(
    habit: Habit,
    month: YearMonth,
    logs: List<com.baverika.r_journal.data.local.entity.HabitLog>,
    onToggleDate: (LocalDate, Boolean) -> Unit
) {
    // Calculate Stats
    val totalDays = (1..month.lengthOfMonth()).map { month.atDay(it) }
    
    // Scheduled days: Matches frequency AND is after creation date
    val scheduledDays = totalDays.count { date ->
        habit.frequency.contains(date.dayOfWeek.value) &&
        !date.isBefore(java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
    }
    
    // Completed days: Has log AND is considered a scheduled day (to prevent >100%)
    val completedDays = totalDays.count { date ->
        val isScheduled = habit.frequency.contains(date.dayOfWeek.value) &&
            !date.isBefore(java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
            
        if (isScheduled) {
             logs.any { it.dateMillis == date.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000 }
        } else {
            false
        }
    }
    
    val percentage = if (scheduledDays > 0) (completedDays.toFloat() / scheduledDays * 100).toInt().coerceAtMost(100) else 0

    // Header: Month Name + Year + Percentage
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "${month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${month.year}",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$percentage% Completed",
            style = MaterialTheme.typography.titleMedium,
            color = Color(habit.color),
            fontWeight = FontWeight.SemiBold
        )
    }
    
    Spacer(modifier = Modifier.height(24.dp))

    // Weekday Headers
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        days.forEach { day ->
            Text(
                text = day,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(40.dp), // Fixed width to align with day blocks
                textAlign = TextAlign.Center
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))

    // Calendar Grid Logic
    val firstDay = month.atDay(1)
    val startPadding = firstDay.dayOfWeek.value - 1
    
    val daysInMonth = month.lengthOfMonth()
    
    // Create straight list of items to render
    // null = padding, LocalDate = actual day
    val gridItems = buildList {
        repeat(startPadding) { add(null) }
        for (i in 1..daysInMonth) {
            add(month.atDay(i))
        }
    }
    
    val weeks = gridItems.chunked(7)
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        weeks.forEach { week ->
            Row(
                 modifier = Modifier.fillMaxWidth(),
                 horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Ensure 7 items per row
                for (i in 0 until 7) {
                    if (i < week.size) {
                        val date = week[i]
                        if (date != null) {
                            DayDetailBlock(
                                date = date,
                                habit = habit,
                                isCompleted = logs.any { it.dateMillis == date.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000 },
                                onClick = { isDone -> onToggleDate(date, isDone) } // Pass 'date' explicitly captured in this scope
                            )
                        } else {
                            Spacer(modifier = Modifier.size(40.dp))
                        }
                    } else {
                         Spacer(modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DayDetailBlock(
    date: LocalDate,
    habit: Habit,
    isCompleted: Boolean,
    onClick: (Boolean) -> Unit
) {
    val habitColor = Color(habit.color)
    val startOfDay = date.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond() * 1000
    
    val isScheduled = habit.frequency.contains(date.dayOfWeek.value)
    // Approx start date check
    val habitStartDate = java.time.Instant.ofEpochMilli(habit.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val isBeforeStart = date.isBefore(habitStartDate)

    val isDisabled = !isScheduled 
    
    val backgroundColor = when {
        isCompleted -> habitColor
        isDisabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) // Greyed out
        else -> Color.Transparent 
    }
    
    val borderColor = when {
        isCompleted -> habitColor
        isDisabled -> Color.Transparent
        else -> habitColor.copy(alpha = 0.5f)
    }
    
    val textColor = when {
        isCompleted -> MaterialTheme.colorScheme.surface
        isDisabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .size(40.dp) // Larger touch target
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(enabled = !isDisabled) { onClick(!isCompleted) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
