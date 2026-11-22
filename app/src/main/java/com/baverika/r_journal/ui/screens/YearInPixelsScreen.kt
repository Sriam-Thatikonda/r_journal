package com.baverika.r_journal.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LocalAppTheme
import com.baverika.r_journal.ui.viewmodel.YearInPixelsViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

enum class HeatmapViewMode {
    MATRIX, RIBBON
}

data class MoodDescriptor(val name: String, val emoji: String, val colorDark: Color, val colorLight: Color)

val MOOD_DESCRIPTORS = listOf(
    MoodDescriptor("Happy", "☀️", Color(0xFFFFC107), Color(0xFFFFB300)),
    MoodDescriptor("Calm", "🌿", Color(0xFF26A69A), Color(0xFF00897B)),
    MoodDescriptor("Anxious", "⚡", Color(0xFFAB47BC), Color(0xFF8E24AA)),
    MoodDescriptor("Sad", "🌧️", Color(0xFF42A5F5), Color(0xFF1E88E5)),
    MoodDescriptor("Tired", "🌙", Color(0xFF78909C), Color(0xFF546E7A))
)

@Composable
fun getMoodColors(): Map<String, Color> {
    val currentTheme = LocalAppTheme.current
    val isDarkTheme = currentTheme != AppTheme.LIGHT && currentTheme != AppTheme.CLOUD_DANCER
    return MOOD_DESCRIPTORS.associate { it.name to if (isDarkTheme) it.colorDark else it.colorLight }
}

@Composable
fun getEmptyCellColor(): Color {
    val currentTheme = LocalAppTheme.current
    return when (currentTheme) {
        AppTheme.LIGHT -> Color(0xFFEBEDF0)
        AppTheme.CLOUD_DANCER -> Color(0xFFE4E0D6)
        AppTheme.MIDNIGHT -> Color(0xFF161B22)
        AppTheme.OCEAN -> Color(0xFF0D2137)
        AppTheme.ROSEWOOD -> Color(0xFF14161A)
        AppTheme.BLUE_SKY -> Color(0xFF081428)
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun YearInPixelsScreen(
    viewModel: YearInPixelsViewModel,
    navController: androidx.navigation.NavController
) {
    val moodMap by viewModel.moodMap.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val moodColors = getMoodColors()
    val emptyCellColor = getEmptyCellColor()

    var viewMode by remember { mutableStateOf(HeatmapViewMode.MATRIX) }
    var selectedDateForInspection by remember { mutableStateOf<LocalDate?>(null) }

    val firstDayOfYear = LocalDate.of(selectedYear, 1, 1)
    val lastDayOfYear = LocalDate.of(selectedYear, 12, 31)

    // Weeks calculation for 52-week ribbon view
    val weeksData = remember(selectedYear) {
        val weeks = mutableListOf<List<LocalDate?>>()
        var currentDate = firstDayOfYear
        while (currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
            currentDate = currentDate.minusDays(1)
        }
        while (currentDate <= lastDayOfYear || currentDate.year == selectedYear) {
            val week = mutableListOf<LocalDate?>()
            for (i in 0..6) {
                val date = currentDate.plusDays(i.toLong())
                week.add(if (date.year == selectedYear) date else null)
            }
            weeks.add(week)
            currentDate = currentDate.plusWeeks(1)
            if (currentDate.year > selectedYear && week.all { it == null || it.year != selectedYear }) {
                break
            }
        }
        weeks
    }

    val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- TOP BAR: YEAR SELECTOR & VIEW MODE TOGGLE ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Year navigation row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.updateYear(selectedYear - 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$selectedYear",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.updateYear(selectedYear + 1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                    }
                }

                // View Mode Segmented Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = viewMode == HeatmapViewMode.MATRIX,
                        onClick = { viewMode = HeatmapViewMode.MATRIX },
                        label = { Text("12-Month Matrix") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = viewMode == HeatmapViewMode.RIBBON,
                        onClick = { viewMode = HeatmapViewMode.RIBBON },
                        label = { Text("52-Week Ribbon") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.ViewWeek,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- MAIN CANVAS: MATRIX VS RIBBON ---
        if (viewMode == HeatmapViewMode.MATRIX) {
            // 12-Month Matrix View (Vertical Grid of Months)
            TwelveMonthMatrix(
                year = selectedYear,
                moodMap = moodMap,
                moodColors = moodColors,
                emptyCellColor = emptyCellColor,
                onDayClick = { date -> selectedDateForInspection = date }
            )
        } else {
            // 52-Week Ribbon View (Horizontal Scrollable Heatmap)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "52-Week Annual Flow",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Scroll horizontally to explore weekly streaks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Day labels column
                        Column(modifier = Modifier.padding(end = 6.dp)) {
                            Spacer(modifier = Modifier.height(18.dp))
                            dayLabels.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 28.dp, height = 18.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = day.take(1),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Weeks columns
                        Column {
                            // Month labels row
                            Row(modifier = Modifier.height(18.dp)) {
                                var currentMonth = 0
                                weeksData.forEachIndexed { weekIndex, week ->
                                    val firstDateOfWeek = week.filterNotNull().firstOrNull()
                                    val showMonthLabel = firstDateOfWeek != null &&
                                            (weekIndex == 0 || firstDateOfWeek.monthValue != currentMonth)

                                    if (firstDateOfWeek != null) {
                                        currentMonth = firstDateOfWeek.monthValue
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(width = 18.dp, height = 18.dp)
                                            .padding(horizontal = 1.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        if (showMonthLabel && firstDateOfWeek != null) {
                                            Text(
                                                text = monthLabels[firstDateOfWeek.monthValue - 1].take(3),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // Heatmap cells grid
                            Row {
                                weeksData.forEach { week ->
                                    Column {
                                        week.forEach { date ->
                                            val cellColor = if (date != null) {
                                                val mood = moodMap[date]
                                                moodColors[mood] ?: emptyCellColor
                                            } else {
                                                Color.Transparent
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .padding(1.5.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(cellColor)
                                                    .then(
                                                        if (date != null) {
                                                            Modifier
                                                                .border(
                                                                    width = 0.5.dp,
                                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                                                    shape = RoundedCornerShape(3.dp)
                                                                )
                                                                .clickable { selectedDateForInspection = date }
                                                        } else Modifier
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- MOOD LEGEND CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Mood Legend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LegendItem(color = emptyCellColor, label = "No entry", showBorder = true)
                    MOOD_DESCRIPTORS.forEach { desc ->
                        LegendItem(
                            color = moodColors[desc.name] ?: desc.colorDark,
                            label = "${desc.emoji} ${desc.name}",
                            showBorder = false
                        )
                    }
                }
            }
        }

        // --- YEAR SUMMARY ---
        val totalEntries = moodMap.count { it.key.year == selectedYear }
        val moodCounts = moodMap.filter { it.key.year == selectedYear }
            .values.groupingBy { it }.eachCount()

        if (totalEntries > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Annual Mood Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$totalEntries days tracked",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    moodCounts.entries.sortedByDescending { it.value }.forEach { (mood, count) ->
                        val desc = MOOD_DESCRIPTORS.find { it.name.equals(mood, ignoreCase = true) }
                        val emoji = desc?.emoji ?: "✨"
                        val percent = if (totalEntries > 0) (count * 100) / totalEntries else 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(moodColors[mood] ?: emptyCellColor)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "$emoji $mood",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$count days ($percent%)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // --- DAY INSPECTION & QUICK MOOD SELECTOR DIALOG ---
    if (selectedDateForInspection != null) {
        val targetDate = selectedDateForInspection!!
        val currentMood = moodMap[targetDate]
        val dateFormatted = targetDate.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))

        AlertDialog(
            onDismissRequest = { selectedDateForInspection = null },
            title = {
                Column {
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!currentMood.isNullOrBlank()) "Logged Mood: $currentMood" else "No mood logged for this day",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Select or update your mood:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(MOOD_DESCRIPTORS.size) { index ->
                            val desc = MOOD_DESCRIPTORS[index]
                            val isSelected = currentMood.equals(desc.name, ignoreCase = true)
                            val color = moodColors[desc.name] ?: desc.colorDark

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) color.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = BorderStroke(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) color else MaterialTheme.colorScheme.outlineVariant
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.setMood(targetDate, desc.name)
                                        selectedDateForInspection = null
                                    }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                    Text(text = desc.emoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = desc.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    if (!currentMood.isNullOrBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.clearMood(targetDate)
                                selectedDateForInspection = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Mood Entry")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDateForInspection = null }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * 12-Month Matrix Component displaying mini monthly calendars in a 2-column or 1-column layout
 */
@Composable
private fun TwelveMonthMatrix(
    year: Int,
    moodMap: Map<LocalDate, String>,
    moodColors: Map<String, Color>,
    emptyCellColor: Color,
    onDayClick: (LocalDate) -> Unit
) {
    val months = (1..12).map { YearMonth.of(year, it) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        months.chunked(2).forEach { monthPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                monthPair.forEach { ym ->
                    MiniMonthCard(
                        yearMonth = ym,
                        moodMap = moodMap,
                        moodColors = moodColors,
                        emptyCellColor = emptyCellColor,
                        onDayClick = onDayClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (monthPair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniMonthCard(
    yearMonth: YearMonth,
    moodMap: Map<LocalDate, String>,
    moodColors: Map<String, Color>,
    emptyCellColor: Color,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val monthTitle = yearMonth.format(DateTimeFormatter.ofPattern("MMMM"))
    val firstDay = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val startDayOffset = firstDay.dayOfWeek.value % 7 // Sunday = 0

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = monthTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Day headers: S M T W T F S
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { initial ->
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 7-column day grid
            val totalCells = startDayOffset + daysInMonth
            val rows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (colIndex in 0..6) {
                            val cellIndex = rowIndex * 7 + colIndex
                            val dayNum = cellIndex - startDayOffset + 1

                            if (dayNum in 1..daysInMonth) {
                                val date = yearMonth.atDay(dayNum)
                                val mood = moodMap[date]
                                val color = moodColors[mood] ?: emptyCellColor

                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                        .border(
                                            0.5.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            RoundedCornerShape(3.dp)
                                        )
                                        .clickable { onDayClick(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (mood != null) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    showBorder: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
                .then(
                    if (showBorder) {
                        Modifier.border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(3.dp)
                        )
                    } else Modifier
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
