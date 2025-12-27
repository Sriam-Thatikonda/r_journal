package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.ui.viewmodel.HabitViewModel
import kotlinx.coroutines.flow.collectLatest
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    journalRepo: JournalRepository,
    habitViewModel: HabitViewModel,
    onYearInPixelsClick: () -> Unit
) {
    var totalEntries by remember { mutableStateOf(0) }
    var currentStreak by remember { mutableStateOf(0) }
    var longestStreak by remember { mutableStateOf(0) }
    var thisMonthCount by remember { mutableStateOf(0) }

    // Collect Streak Stats
    LaunchedEffect(Unit) {
        journalRepo.allEntries.collectLatest { entries ->
            totalEntries = entries.size

            // Calculate dates list
            val entryDates = entries.map { entry ->
                java.time.Instant.ofEpochMilli(entry.dateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }

            currentStreak = calculateCurrentStreak(entryDates)
            longestStreak = calculateLongestStreak(entryDates)

            // Count this month
            val now = LocalDate.now()
            thisMonthCount = entries.count { entry ->
                val date = java.time.Instant.ofEpochMilli(entry.dateMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                date.month == now.month && date.year == now.year
            }
        }
    }

    // Collect Mood Stats
    val moodStats by journalRepo.moodStats.collectAsState(initial = JournalRepository.MoodStats(0f, 0f))

    // Collect Habit Stats
    val habits by habitViewModel.habitState.collectAsState()
    val completedCount = habits.count { it.isCompleted }
    val totalHabits = habits.size
    val completionRate = if (totalHabits > 0) completedCount.toFloat() / totalHabits else 0f
    
    // Collect Weekly Stats
    val weeklyStats by habitViewModel.weeklyStats.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // Enable scrolling for smaller screens
    ) {
        Text(
            "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // --- Streak Section ---
        
        // Streak card (prominent)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "$currentStreak",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    text = if (currentStreak == 1) "day streak" else "day streak",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                if (currentStreak > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Keep it going!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.EmojiEvents,
                label = "Best Streak",
                value = "$longestStreak",
                subtitle = if (longestStreak == 1) "day" else "days"
            )

            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarMonth,
                label = "This Month",
                value = "$thisMonthCount",
                subtitle = if (thisMonthCount == 1) "entry" else "entries"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.MenuBook,
                label = "Total Entries",
                value = "$totalEntries",
                subtitle = if (totalEntries == 1) "entry" else "entries"
            )
            
            // Spacer for alignment if needed, or another stat
             Spacer(modifier = Modifier.weight(1f))
        }

        // --- Habits Section ---
        Text(
            text = "Today's Habits",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )

        // Habit Progress Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    progress = completionRate,
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "${(completionRate * 100).toInt()}% Completed",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "$completedCount of $totalHabits habits done",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        // Quick Toggle List (Limited to 5 to avoid nested scroll issues)
        if (habits.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    habits.take(5).forEach { habit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = habit.isCompleted,
                                onCheckedChange = { isChecked ->
                                    habitViewModel.toggleHabit(habit.habit.id, isChecked)
                                }
                            )
                            Text(
                                text = habit.habit.title,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    if (habits.size > 5) {
                        Text(
                            text = "+ ${habits.size - 5} more habits",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 12.dp, bottom = 8.dp)
                        )
                    }
                }
            }
        } else {
            Text(
                text = "No habits for today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // --- Mood Section ---

        Text(
            text = "Mood Insights",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )

        StatsSummaryCard(stats = moodStats)

        Spacer(modifier = Modifier.height(16.dp))

        // Year in Pixel Button
        OutlinedButton(
            onClick = onYearInPixelsClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Year in Pixels")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // --- Habit History Chart ---
        HabitHistoryChart(stats = weeklyStats)
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatsSummaryCard(stats: JournalRepository.MoodStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Weekly Avg", score = stats.weeklyAverage)
            StatItem(label = "Monthly Avg", score = stats.monthlyAverage)
        }
    }
}

@Composable
fun StatItem(label: String, score: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        val color = when {
            score >= 4.0f -> MaterialTheme.colorScheme.primary
            score >= 2.5f -> MaterialTheme.colorScheme.secondary
            score > 0f -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        }

        val displayScore = if (score > 0) String.format("%.1f", score) else "-"

        Text(
            text = displayScore,
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
    }
}

// Helper functions for streak calculation
fun calculateCurrentStreak(dates: List<LocalDate>): Int {
    if (dates.isEmpty()) return 0
    
    val sortedDates = dates.distinct().sortedDescending()
    val today = LocalDate.now()
    
    // Check if the streak is active (entry today or yesterday)
    if (sortedDates.first() != today && sortedDates.first() != today.minusDays(1)) {
        return 0
    }
    
    var streak = 1
    var currentDate = sortedDates.first()
    
    for (i in 1 until sortedDates.size) {
        if (sortedDates[i] == currentDate.minusDays(1)) {
            streak++
            currentDate = sortedDates[i]
        } else {
            break
        }
    }
    
    return streak
}

fun calculateLongestStreak(dates: List<LocalDate>): Int {
    if (dates.isEmpty()) return 0
    
    val sortedDates = dates.distinct().sorted()
    var maxStreak = 1
    var currentStreak = 1
    
    for (i in 0 until sortedDates.size - 1) {
        if (sortedDates[i+1] == sortedDates[i].plusDays(1)) {
            currentStreak++
        } else {
            maxStreak = maxOf(maxStreak, currentStreak)
            currentStreak = 1
        }
    }
    
    return maxOf(maxStreak, currentStreak)
}

@Composable
fun HabitHistoryChart(stats: List<Pair<LocalDate, Int>>) {
    if (stats.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Habit History (Last 7 Days)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            val maxCount = stats.maxOfOrNull { it.second } ?: 1
            val chartHeight = 150.dp
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEach { (date, count) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Bar
                        val barHeightRatio = if (maxCount > 0) count.toFloat() / maxCount else 0f
                        
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(barHeightRatio.coerceAtLeast(0.02f)) // Min height for visibility (tiny dot)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(if (count > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) // Grey if 0
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Date Label
                        Text(
                            text = date.format(java.time.format.DateTimeFormatter.ofPattern("EEE")),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Count Label
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (count > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}