package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
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
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.data.local.entity.TrackerHistory
import com.baverika.r_journal.ui.viewmodel.TrackerViewModel
import com.baverika.r_journal.utils.ColorUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDetailsScreen(
    trackerId: String,
    viewModel: TrackerViewModel,
    navController: NavController
) {
    val trackerFlow = remember(trackerId) { viewModel.getTrackerByIdFlow(trackerId) }
    val trackerState by trackerFlow.collectAsState(initial = null)

    val historyFlow = remember(trackerId) { viewModel.getHistoryForTracker(trackerId) }
    val historyList by historyFlow.collectAsState(initial = emptyList())

    val currentTracker = trackerState

    if (currentTracker == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val backgroundColor = Color(currentTracker.color)
    val textColor = ColorUtils.getContrastingTextColor(backgroundColor)
    val secondaryTextColor = ColorUtils.getSecondaryTextColor(backgroundColor)

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormatter = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentTracker.title, color = textColor) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor)
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("edit_tracker/${currentTracker.id}") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Tracker", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Large Emoji display
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(textColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(currentTracker.emoji, style = MaterialTheme.typography.displayLarge)
            }

            // Big Counter Display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${currentTracker.currentCount}",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp, fontWeight = FontWeight.Bold),
                    color = textColor
                )
                Text(
                    text = "Goal: ${currentTracker.goal}",
                    style = MaterialTheme.typography.titleMedium,
                    color = secondaryTextColor
                )
                Text(
                    text = "Increment Step: ${currentTracker.incrementStep} | Reset: ${currentTracker.resetFrequency}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryTextColor
                )
            }

            // Circular/Linear progress indicator
            val progress = if (currentTracker.goal > 0) {
                (currentTracker.currentCount.toFloat() / currentTracker.goal.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
            val progressPercent = (progress * 100).toInt()

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = textColor,
                trackColor = textColor.copy(alpha = 0.2f)
            )

            // Primary Control Buttons (Large)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { viewModel.incrementTracker(currentTracker.id) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = textColor,
                        contentColor = backgroundColor
                    ),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add ${currentTracker.incrementStep}", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.decrementTracker(currentTracker.id) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = textColor
                    ),
                    border = BorderStroke(2.dp, textColor),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subtract ${currentTracker.incrementStep}", fontWeight = FontWeight.Bold)
                }
            }

            // Reset Button
            Button(
                onClick = { viewModel.resetTracker(currentTracker.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = textColor.copy(alpha = 0.2f),
                    contentColor = textColor
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset Counter", fontWeight = FontWeight.Bold)
            }

            // Notes Section
            if (!currentTracker.notes.isNullOrBlank()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            currentTracker.notes,
                            style = MaterialTheme.typography.bodyLarge,
                            color = textColor.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // ── Live Statistics Section ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Statistics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Goal Completion", style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                        Text("$progressPercent%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Created Date", style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                        Text(dateFormatter.format(Date(currentTracker.createdDate)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textColor)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Last Updated", style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                        Text(timeFormatter.format(Date(currentTracker.updatedDate)), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textColor)
                    }

                    if (historyList.isNotEmpty()) {
                        val totalRecorded = historyList.sumOf { it.value }
                        val avgRecorded = (totalRecorded.toFloat() / historyList.size).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Historical Average", style = MaterialTheme.typography.bodyMedium, color = secondaryTextColor)
                            Text("$avgRecorded / reset period", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = textColor)
                        }
                    }
                }
            }

            // ── History Logs Section ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = textColor.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = textColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "History Logs",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    if (historyList.isEmpty()) {
                        Text(
                            text = "No recorded history yet.\nCompleted values will log automatically when the reset schedule triggers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryTextColor.copy(alpha = 0.8f)
                        )
                    } else {
                        historyList.take(5).forEach { history ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(history.date, style = MaterialTheme.typography.bodyMedium, color = textColor)
                                Text("Count: ${history.value}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
                            }
                        }
                    }
                }
            }
        }
    }
}
