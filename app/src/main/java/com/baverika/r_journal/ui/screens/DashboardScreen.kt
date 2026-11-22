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
    onYearInPixelsClick: () -> Unit,
    onNavigate: ((String) -> Unit)? = null
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
        // --- ZONE 1: HERO PULSE & MOMENTUM ---

        // Year Milestone Trigger Bar
        Surface(
            onClick = { showMilestoneDialog = true },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
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

        // Streak Hero Card
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
                    .padding(vertical = 22.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
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
                    text = if (entrySummary.currentStreak > 0) "Keep the momentum going!" else "Write today's entry to ignite your streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }
        }

        // Stats Triad
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

        // --- ZONE 2: DAILY FOCUS & HABITS PROGRESS ---
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Today's Habits")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
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
                        modifier = Modifier.size(52.dp),
                        strokeWidth = 5.5.dp,
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

        // 7-Day Habit History Chart
        HabitHistoryChart(stats = weeklyStats)

        // Mood Insights
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Mood Insights")
            StatsSummaryCard(stats = moodStats)
        }

        // --- ZONE 3: DEEP DIVES & EXPLORATION ---
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("Deep Dives & Insights")

            // Year in Pixels Action Card
            DeepDiveActionCard(
                title = "Year in Pixels",
                subtitle = "365-day annual mood heatmap & emotional matrix",
                icon = Icons.Default.GridOn,
                badge = "Mood Canvas",
                onClick = onYearInPixelsClick
            )

            if (onNavigate != null) {
                // Challenges Action Card
                DeepDiveActionCard(
                    title = "Challenge Quests",
                    subtitle = "Structured multi-day habit sprints and roadmaps",
                    icon = Icons.Default.TrackChanges,
                    badge = "Sprint",
                    onClick = { onNavigate("challenges") }
                )

                // Craving Quest Card
                DeepDiveActionCard(
                    title = "Craving Quests",
                    subtitle = "Urge surfing, mindful regulation & impulse challenges",
                    icon = Icons.Default.Shield,
                    badge = "Mindfulness",
                    onClick = { onNavigate("craving_quest") }
                )

                // Life Milestones Card
                DeepDiveActionCard(
                    title = "Life Milestones",
                    subtitle = "Elapsed days, counters and life event timelines",
                    icon = Icons.Default.HourglassBottom,
                    badge = "Milestones",
                    onClick = { onNavigate("life_trackers") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DeepDiveActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
