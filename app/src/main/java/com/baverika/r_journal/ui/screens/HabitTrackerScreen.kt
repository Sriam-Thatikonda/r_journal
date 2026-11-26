package com.baverika.r_journal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.components.EmptyState
import com.baverika.r_journal.ui.theme.JetBrainsMonoFamily
import com.baverika.r_journal.ui.theme.MotionSpec
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
    val streaks by viewModel.habitStreaks.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Header for 7 Days (Past 6 days + Today)
        val titleColumnWidth = 112.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(titleColumnWidth)
                    .padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Habit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val today = java.time.LocalDate.now()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (6 downTo 0).forEach { i ->
                    val date = today.minusDays(i.toLong())
                    val isToday = i == 0
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(36.dp)
                    ) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.getDefault()).take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        if (dashboardGrids.isEmpty()) {
             EmptyState(
                 icon = Icons.Default.TrackChanges,
                 title = "No Habits Yet",
                 message = "Track daily routines and build consistency.",
                 actionLabel = "Add Habit",
                 onActionClick = { navController.navigate("add_habit") }
             )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(dashboardGrids) { grid ->
                    HabitRow7Day(
                        grid = grid,
                        streak = streaks[grid.habit.id] ?: 0,
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
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HabitRow7Day(
    grid: HabitViewModel.YearlyHabitGrid,
    titleColumnWidth: androidx.compose.ui.unit.Dp = 112.dp,
    streak: Int = 0,
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
            .height(70.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name Column
        Box(
            modifier = Modifier
                .width(titleColumnWidth)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onClick = onNameClick,
                    onLongClick = { showMenu = true }
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = grid.habit.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (streak > 0) {
                    Text(
                        text = "🔥 $streak d",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val appTheme = com.baverika.r_journal.ui.theme.LocalAppTheme.current
            grid.days.forEach { dayState ->
                Box(
                    modifier = Modifier.width(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HeatmapBlock(
                        state = dayState,
                        color = com.baverika.r_journal.ui.theme.getEffectiveColor(Color(grid.habit.color), appTheme),
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
}

@Composable
fun HeatmapBlock(
    state: HabitViewModel.DayState,
    color: Color,
    isLarge: Boolean = false,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDone = state.status == HabitViewModel.HabitStatus.DONE

    val backgroundColor by animateColorAsState(
        targetValue = when (state.status) {
            HabitViewModel.HabitStatus.DONE -> color
            HabitViewModel.HabitStatus.DISABLED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            HabitViewModel.HabitStatus.PENDING -> Color.Transparent
        },
        animationSpec = tween(220),
        label = "habitCellBg"
    )

    val borderColor = when (state.status) {
        HabitViewModel.HabitStatus.DONE -> color
        HabitViewModel.HabitStatus.DISABLED -> Color.Transparent
        HabitViewModel.HabitStatus.PENDING -> color.copy(alpha = 0.35f)
    }

    val cellScale by animateFloatAsState(
        targetValue = if (isDone) 1.0f else 0.94f,
        animationSpec = MotionSpec.tactileSpring(),
        label = "habitCellScale"
    )

    val size = if (isLarge) 32.dp else 18.dp
    val radius = if (isLarge) 10.dp else 3.dp

    Box(
        modifier = Modifier
            .size(size)
            .scale(cellScale)
            .clip(RoundedCornerShape(radius))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(radius))
            .clickable(enabled = state.status != HabitViewModel.HabitStatus.DISABLED) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        if (isDone && isLarge) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(18.dp)
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
    val appTheme = com.baverika.r_journal.ui.theme.LocalAppTheme.current
    val color = com.baverika.r_journal.ui.theme.getEffectiveColor(Color(habit.color), appTheme)
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
