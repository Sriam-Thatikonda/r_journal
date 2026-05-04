package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.viewmodel.HabitUiState
import com.baverika.r_journal.ui.viewmodel.HabitViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HabitTrackerScreen(
    viewModel: HabitViewModel,
    navController: NavController
) {
    val dashboardGrids by viewModel.dashboardHabitGrids.collectAsState()
    val yearlyGrids by viewModel.yearlyHabitGrids.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header for 7 Days
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 120.dp, bottom = 8.dp, top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
             val today = java.time.LocalDate.now()
             (0..6).forEach { i ->
                 val date = today.plusDays(i.toLong())
                 Column(
                     horizontalAlignment = Alignment.CenterHorizontally,
                     modifier = Modifier.width(32.dp)
                 ) {
                     Text(
                         text = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()),
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                     Text(
                         text = date.dayOfMonth.toString(),
                         style = MaterialTheme.typography.labelSmall,
                         fontWeight = FontWeight.Bold
                     )
                 }
             }
        }
        
        Divider()

        if (dashboardGrids.isEmpty()) {
             Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 Text("No habits yet. Tap + to add one.")
             }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(dashboardGrids) { grid ->
                    HabitRow7Day(
                        grid = grid,
                        onNameClick = { navController.navigate("habit_year_overview/${grid.habit.id}") },
                        onEditClick = { habit ->
                            navController.navigate("add_habit?habitId=${habit.id}")
                        },
                        onDeleteClick = { habit ->
                            viewModel.deleteHabit(habit)
                        },
                        onBlockClick = { date, isCompleted ->
                            viewModel.toggleHabitForDate(grid.habit.id, date, isCompleted)
                        }
                    )
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitRow7Day(
    grid: HabitViewModel.YearlyHabitGrid,
    onNameClick: () -> Unit,
    onEditClick: (com.baverika.r_journal.data.local.entity.Habit) -> Unit,
    onDeleteClick: (com.baverika.r_journal.data.local.entity.Habit) -> Unit,
    onBlockClick: (java.time.LocalDate, Boolean) -> Unit
) {
    // Context Menu State
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name Column
        Box(
            modifier = Modifier
                .width(120.dp) // Slightly wider for better readability
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = onNameClick,
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center // Changed from Alignment.CenterStart
        ) {
            Text(
                text = grid.habit.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = {
                        showMenu = false
                        onEditClick(grid.habit)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDeleteClick(grid.habit)
                    }
                )
            }
        }
        
        // 7 Day Grid
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            grid.days.forEach { dayState ->
                HeatmapBlock(
                    state = dayState,
                    color = Color(grid.habit.color),
                    isLarge = true, // Larger blocks for touch focus
                    onClick = {
                        if (dayState.status != HabitViewModel.HabitStatus.DISABLED) {
                            val newState = dayState.status != HabitViewModel.HabitStatus.DONE
                            onBlockClick(dayState.date, newState)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun HeatmapBlock(
    state: HabitViewModel.DayState,
    color: Color,
    isLarge: Boolean = false,
    onClick: () -> Unit
) {
    val backgroundColor = when (state.status) {
        HabitViewModel.HabitStatus.DONE -> color
        HabitViewModel.HabitStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        HabitViewModel.HabitStatus.PENDING -> Color.Transparent
    }
    
    val borderColor = when (state.status) {
        HabitViewModel.HabitStatus.DONE -> color
        HabitViewModel.HabitStatus.DISABLED -> Color.Transparent
        HabitViewModel.HabitStatus.PENDING -> color.copy(alpha = 0.3f)
    }

    val size = if (isLarge) 32.dp else 18.dp
    val radius = if (isLarge) 8.dp else 2.dp

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(radius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(radius))
            .clickable(enabled = state.status != HabitViewModel.HabitStatus.DISABLED) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (state.status == HabitViewModel.HabitStatus.DONE && isLarge) {
             Icon(
                 Icons.Default.Check, 
                 contentDescription = null, 
                 tint = MaterialTheme.colorScheme.surface,
                 modifier = Modifier.size(20.dp)
             )
        }
    }
}

@Composable
fun CompactHabitTracker(
    viewModel: HabitViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habitState.collectAsState()
    
    if (habits.isNotEmpty()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Habits",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                habits.forEach { habitState ->
                    CompactHabitItem(
                        habitState = habitState,
                        onToggle = { isChecked ->
                            viewModel.toggleHabit(habitState.habit.id, isChecked)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CompactHabitItem(
    habitState: HabitUiState,
    onToggle: (Boolean) -> Unit
) {
    val habit = habitState.habit
    val color = Color(habit.color)
    val isCompleted = habitState.isCompleted

    Surface(
        onClick = { onToggle(!isCompleted) },
        shape = RoundedCornerShape(8.dp),
        color = if (isCompleted) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = if (isCompleted) null else BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = habit.title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isCompleted) color else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
