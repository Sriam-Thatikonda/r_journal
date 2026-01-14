package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.baverika.r_journal.ui.viewmodel.HabitViewModel
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitYearOverviewScreen(
    viewModel: HabitViewModel,
    navController: NavController,
    habitId: String
) {
    val yearlyGrids by viewModel.yearlyHabitGrids.collectAsState()
    val habitGrid = yearlyGrids.find { it.habit.id == habitId }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = habitGrid?.habit?.title ?: "Overview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (habitGrid == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (yearlyGrids.isEmpty()) CircularProgressIndicator() else Text("Habit not found")
            }
        } else {
            // Group days by month and then chunk into pairs for 2-column layout
            val daysByMonth = habitGrid.days.groupBy { it.date.month }
            val months = daysByMonth.entries.toList()
            val chunkedMonths = months.chunked(2)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing
            ) {
                items(chunkedMonths.size) { index ->
                    val pair = chunkedMonths[index]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // First Month
                        Box(modifier = Modifier.weight(1f)) {
                            MonthOverviewCard(
                                monthName = pair[0].key.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                days = pair[0].value,
                                color = Color(habitGrid.habit.color),
                                onClick = { 
                                    // Pass month index (1-12)
                                    navController.navigate("habit_detail/$habitId/${pair[0].key.value}") 
                                }
                            )
                        }

                        // Second Month (if exists)
                        if (pair.size > 1) {
                            Box(modifier = Modifier.weight(1f)) {
                                MonthOverviewCard(
                                    monthName = pair[1].key.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                    days = pair[1].value,
                                    color = Color(habitGrid.habit.color),
                                    onClick = { 
                                        navController.navigate("habit_detail/$habitId/${pair[1].key.value}") 
                                    }
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthOverviewCard(
    monthName: String,
    days: List<HabitViewModel.DayState>,
    color: Color,
    onClick: () -> Unit
) {
    // Calculate stats
    val scheduled = days.count { it.status != HabitViewModel.HabitStatus.DISABLED }
    val completed = days.count { it.status == HabitViewModel.HabitStatus.DONE }
    val percentage = if (scheduled > 0) (completed.toFloat() / scheduled * 100).toInt() else 0

    // Calendar logic: Pad the start of the month with empty blocks based on day of week
    // Day of week: 1=Mon, 7=Sun.
    // If we want Grid to start at Mon:
    val firstDay = days.first().date
    val startPadding = firstDay.dayOfWeek.value - 1 
    
    // Create a list including padding for the grid
    val gridItems = buildList {
        repeat(startPadding) { add(null) } // Padding
        addAll(days)
        // Optionally pad end? No need for simple Grid
    }
    
    val chunkedWeeks = gridItems.chunked(7)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                chunkedWeeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween // Even spacing
                    ) {
                        // Ensure we always have 7 slots even if last week is partial
                        for (i in 0 until 7) {
                            if (i < week.size) {
                                val dayState = week[i]
                                if (dayState != null) {
                                    MiniDayBlock(dayState, color)
                                } else {
                                    // Padding block (invisible)
                                    Spacer(modifier = Modifier.size(16.dp)) 
                                }
                            } else {
                                // Trailing empty days in last week
                                Spacer(modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniDayBlock(
    state: HabitViewModel.DayState,
    color: Color
) {
    val isActive = state.status != HabitViewModel.HabitStatus.DISABLED
    val isDone = state.status == HabitViewModel.HabitStatus.DONE

    val backgroundColor = when (state.status) {
        HabitViewModel.HabitStatus.DONE -> color
        HabitViewModel.HabitStatus.DISABLED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f) // Greyed out
        HabitViewModel.HabitStatus.PENDING -> Color.Transparent
    }
    
    val borderColor = when (state.status) {
        HabitViewModel.HabitStatus.DONE -> color
        HabitViewModel.HabitStatus.DISABLED -> Color.Transparent
        HabitViewModel.HabitStatus.PENDING -> color.copy(alpha = 0.3f) // Visible border for pending
    }
    
    val textColor = when (state.status) {
        HabitViewModel.HabitStatus.DONE -> MaterialTheme.colorScheme.surface // Contrast on color
        HabitViewModel.HabitStatus.DISABLED -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
        HabitViewModel.HabitStatus.PENDING -> MaterialTheme.colorScheme.onSurface // Normal text
    }

    Box(
        modifier = Modifier
            .size(16.dp) // Optimized size for date text
            .clip(RoundedCornerShape(2.dp))
            .background(backgroundColor)
            .border(
                width = if (state.status == HabitViewModel.HabitStatus.PENDING) 1.dp else 0.dp, 
                color = borderColor, 
                shape = RoundedCornerShape(2.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = state.date.dayOfMonth.toString().padStart(2, '0'),
            fontSize = 8.sp, // Slightly increased from 7.sp for better readability
            fontWeight = FontWeight.Medium,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}
