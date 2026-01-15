package com.baverika.r_journal.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Theme-adaptive mood colors for the heatmap
 * Colors are designed to be distinguishable and harmonious across all themes
 */
@Composable
fun getMoodColors(): Map<String, Color> {
    val currentTheme = LocalAppTheme.current
    val isDarkTheme = currentTheme != AppTheme.LIGHT
    
    return if (isDarkTheme) {
        // Dark theme colors - vibrant but not too harsh
        mapOf(
            "Happy" to Color(0xFFFFC107),     // Bright Amber/Gold
            "Calm" to Color(0xFF26A69A),      // Teal
            "Anxious" to Color(0xFFAB47BC),   // Purple
            "Sad" to Color(0xFF42A5F5),       // Blue
            "Tired" to Color(0xFF78909C)      // Blue Grey
        )
    } else {
        // Light theme colors - slightly muted for better readability
        mapOf(
            "Happy" to Color(0xFFFFB300),     // Darker Amber
            "Calm" to Color(0xFF00897B),      // Darker Teal
            "Anxious" to Color(0xFF8E24AA),   // Darker Purple
            "Sad" to Color(0xFF1E88E5),       // Darker Blue
            "Tired" to Color(0xFF546E7A)      // Darker Blue Grey
        )
    }
}

/**
 * Get empty cell color based on theme
 */
@Composable
fun getEmptyCellColor(): Color {
    val currentTheme = LocalAppTheme.current
    return when (currentTheme) {
        AppTheme.LIGHT -> Color(0xFFEBEDF0)
        AppTheme.MIDNIGHT -> Color(0xFF161B22)
        AppTheme.OCEAN -> Color(0xFF0D2137)
        AppTheme.ROSEWOOD -> Color(0xFF2D1F1D)
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
    
    // State for mood selection dialog
    var showMoodDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    
    // Calculate weeks for the selected year
    val firstDayOfYear = LocalDate.of(selectedYear, 1, 1)
    val lastDayOfYear = LocalDate.of(selectedYear, 12, 31)
    val weekFields = WeekFields.of(Locale.getDefault())
    
    // Generate all weeks of the year
    val weeksData = remember(selectedYear) {
        val weeks = mutableListOf<List<LocalDate?>>()
        var currentDate = firstDayOfYear
        
        // Start from the beginning of the first week (may include days from previous year)
        while (currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
            currentDate = currentDate.minusDays(1)
        }
        
        // Generate weeks until we pass the end of the year
        while (currentDate <= lastDayOfYear || currentDate.year == selectedYear) {
            val week = mutableListOf<LocalDate?>()
            for (i in 0..6) { // Sunday to Saturday
                val date = currentDate.plusDays(i.toLong())
                // Only include dates from the selected year
                week.add(if (date.year == selectedYear) date else null)
            }
            weeks.add(week)
            currentDate = currentDate.plusWeeks(1)
            
            // Stop if we've gone past the year
            if (currentDate.year > selectedYear && week.all { it == null || it.year != selectedYear }) {
                break
            }
        }
        weeks
    }
    
    // Month labels for header
    val monthLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", 
                             "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mood Heatmap $selectedYear") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // GitHub-style Heatmap Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Scrollable heatmap container
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Day labels column
                        Column(
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            // Spacer for month labels row
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            dayLabels.forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 28.dp, height = 14.dp)
                                        .padding(vertical = 1.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = day.take(1),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        
                        // Weeks columns
                        Column {
                            // Month labels row
                            Row(
                                modifier = Modifier.height(16.dp)
                            ) {
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
                                            .size(width = 14.dp, height = 16.dp)
                                            .padding(horizontal = 1.dp),
                                        contentAlignment = Alignment.BottomStart
                                    ) {
                                        if (showMonthLabel && firstDateOfWeek != null) {
                                            Text(
                                                text = monthLabels[firstDateOfWeek.monthValue - 1].take(3),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Heatmap grid
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
                                                    .size(12.dp)
                                                    .padding(1.dp)
                                                    .background(
                                                        color = cellColor,
                                                        shape = RoundedCornerShape(2.dp)
                                                    )
                                                    .then(
                                                        if (date != null) {
                                                            Modifier
                                                                .border(
                                                                    width = 0.5.dp,
                                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                                    shape = RoundedCornerShape(2.dp)
                                                                )
                                                                .clickable {
                                                                    selectedDate = date
                                                                    showMoodDialog = true
                                                                }
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Mood Legend Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Mood Legend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Legend items in a flow layout
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Empty cell legend
                        LegendItem(
                            color = emptyCellColor,
                            label = "No entry",
                            showBorder = true
                        )
                        
                        // Mood color legends
                        moodColors.forEach { (mood, color) ->
                            LegendItem(
                                color = color,
                                label = mood,
                                showBorder = false
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Stats summary
            val totalEntries = moodMap.count { it.key.year == selectedYear }
            val moodCounts = moodMap.filter { it.key.year == selectedYear }
                .values.groupingBy { it }.eachCount()
            
            if (totalEntries > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Year Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "$totalEntries days tracked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Mood breakdown
                        moodCounts.entries.sortedByDescending { it.value }.forEach { (mood, count) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = moodColors[mood] ?: emptyCellColor,
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = mood,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Mood Selection Dialog
    if (showMoodDialog && selectedDate != null) {
        AlertDialog(
            onDismissRequest = { showMoodDialog = false },
            title = { 
                Text(
                    text = selectedDate!!.format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                    style = MaterialTheme.typography.titleMedium
                ) 
            },
            text = {
                Column {
                    Text("How was your day?")
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(moodColors.toList().size) { index ->
                            val (mood, color) = moodColors.toList()[index]
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.setMood(selectedDate!!, mood)
                                        showMoodDialog = false
                                    }
                                    .padding(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(color, RoundedCornerShape(4.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(mood, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoodDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Legend item composable for the mood legend
 */
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
                .background(color, RoundedCornerShape(3.dp))
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
