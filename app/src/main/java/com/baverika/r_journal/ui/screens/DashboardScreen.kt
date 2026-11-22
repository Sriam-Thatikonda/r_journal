package com.baverika.r_journal.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
import com.baverika.r_journal.repository.TaskRepository
import com.baverika.r_journal.ui.components.MilestoneCardDialog
import com.baverika.r_journal.ui.viewmodel.HabitViewModel
import com.baverika.r_journal.utils.MilestoneCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    journalRepo: JournalRepository,
    taskRepo: TaskRepository,
    habitViewModel: HabitViewModel,
    onYearInPixelsClick: () -> Unit
) {
    var showMilestoneDialog by remember { mutableStateOf(false) }

    val entries by journalRepo.allEntries.collectAsState(initial = emptyList())
    val tasks by taskRepo.allTasks.collectAsState(initial = emptyList())
    val moodStats by journalRepo.moodStats.collectAsState(initial = JournalRepository.MoodStats(0f, 0f))
    val habits by habitViewModel.habitState.collectAsState()
    val weeklyStats by habitViewModel.weeklyStats.collectAsState()

    val milestoneStats = remember(entries, tasks) {
        MilestoneCalculator.calculateCurrentYearStats(entries, tasks)
    }

    val entrySummary = remember(entries) {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now(zone)
        val dates = entries.map { Instant.ofEpochMilli(it.dateMillis).atZone(zone).toLocalDate() }
        val thisMonthCount = dates.count { it.year == now.year && it.month == now.month }
        EntrySummary(
            totalEntries = entries.size,
            currentStreak = calculateCurrentStreak(dates),
            longestStreak = calculateLongestStreak(dates),
            thisMonthCount = thisMonthCount
        )
    }

    val completedCount = habits.count { it.isCompleted }
    val totalHabits = habits.size
    val targetCompletionRate = if (totalHabits > 0) completedCount.toFloat() / totalHabits else 0f
    val animatedCompletionRate by animateFloatAsState(
        targetValue = targetCompletionRate,
        animationSpec = tween(durationMillis = 450),
        label = "habitCompletion"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Year Milestone Trigger
        Surface(
            onClick = { showMilestoneDialog = true },
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🎉", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${milestoneStats.year} Milestone Card",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "Tap to view your ${milestoneStats.year} story (Jan 1 – Dec 31)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open milestone card",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

        if (showMilestoneDialog) {
            MilestoneCardDialog(
                stats = milestoneStats,
                onDismiss = { showMilestoneDialog = false }
            )
        }

        // Streak Hero & Journal Metrics
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "${entrySummary.currentStreak}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Text(
                        text = if (entrySummary.currentStreak == 1) "day streak" else "days streak",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (entrySummary.currentStreak > 0) "Keep the momentum going" else "Write today's entry to start a streak",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.EmojiEvents,
                    label = "Best Streak",
                    value = "${entrySummary.longestStreak}",
                    subtitle = if (entrySummary.longestStreak == 1) "day" else "days"
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarMonth,
                    label = "This Month",
                    value = "${entrySummary.thisMonthCount}",
                    subtitle = if (entrySummary.thisMonthCount == 1) "entry" else "entries"
                )

                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    label = "All Entries",
                    value = "${entrySummary.totalEntries}",
                    subtitle = if (entrySummary.totalEntries == 1) "entry" else "entries"
                )
            }
        }

        // Today's Habits Section
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Today's Habits")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        progress = { animatedCompletionRate },
                        modifier = Modifier.size(50.dp),
                        strokeWidth = 5.dp,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.16f)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "${(animatedCompletionRate * 100).toInt()}% Completed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = if (totalHabits > 0) {
                                "$completedCount of $totalHabits habits done today"
                            } else {
                                "No habits scheduled for today"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            if (habits.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        habits.take(5).forEachIndexed { index, habit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        habitViewModel.toggleHabit(habit.habit.id, !habit.isCompleted)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = habit.isCompleted,
                                    onCheckedChange = { isChecked ->
                                        habitViewModel.toggleHabit(habit.habit.id, isChecked)
                                    }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = habit.habit.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (habit.isCompleted) FontWeight.Medium else FontWeight.Normal,
                                    color = if (habit.isCompleted) {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (index < habits.take(5).lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                )
                            }
                        }

                        if (habits.size > 5) {
                            Text(
                                text = "+ ${habits.size - 5} more habits in Habits tab",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Mood Insights & Year in Pixels
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Mood Insights")

            StatsSummaryCard(stats = moodStats)

            OutlinedButton(
                onClick = onYearInPixelsClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View Year in Pixels",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 7-Day Habit History Chart
        HabitHistoryChart(stats = weeklyStats)

        Spacer(modifier = Modifier.height(12.dp))
    }
}

private data class EntrySummary(
    val totalEntries: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val thisMonthCount: Int
)

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    subtitle: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatsSummaryCard(stats: JournalRepository.MoodStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(label = "Weekly Avg", score = stats.weeklyAverage)
            VerticalDivider(
                modifier = Modifier.height(44.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
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
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        }

        val displayScore = if (score > 0f) String.format("%.1f", score) else "–"

        Text(
            text = displayScore,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

fun calculateCurrentStreak(dates: List<LocalDate>): Int {
    if (dates.isEmpty()) return 0

    val sortedDates = dates.distinct().sortedDescending()
    val today = LocalDate.now()

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
        if (sortedDates[i + 1] == sortedDates[i].plusDays(1)) {
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

    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEE") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Habit History (Last 7 Days)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val maxCount = (stats.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                stats.forEach { (date, count) ->
                    val ratio = (count.toFloat() / maxCount).coerceIn(0f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = ratio,
                        animationSpec = tween(durationMillis = 400),
                        label = "barRatio_${date}"
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (count > 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (count > 0) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            }
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(18.dp)
                                    .fillMaxHeight(if (count > 0) animatedRatio.coerceAtLeast(0.08f) else 0.04f)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        if (count > 0) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = date.format(dayFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
} 