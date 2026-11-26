package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.ui.components.EmptyState
import com.baverika.r_journal.ui.theme.JetBrainsMonoFamily
import com.baverika.r_journal.ui.theme.pressScale
import com.baverika.r_journal.ui.viewmodel.TrackerViewModel
import com.baverika.r_journal.utils.ColorUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrackersScreen(
    viewModel: TrackerViewModel,
    navController: NavController,
    searchQuery: String = ""
) {
    val trackers by viewModel.trackers.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Tracker?>(null) }

    val filteredTrackers = remember(trackers, searchQuery) {
        if (searchQuery.isBlank()) trackers else trackers.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (trackers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.AddReaction,
                    title = "No Trackers Yet",
                    message = "Create custom counters and daily goals to stay on track.",
                    actionLabel = "Add Tracker",
                    onActionClick = { navController.navigate("add_tracker") }
                )
            } else if (filteredTrackers.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.SearchOff,
                    title = "No Matching Trackers",
                    message = "Nothing matched \"$searchQuery\""
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                ) {
                    items(filteredTrackers, key = { it.id }) { tracker ->
                        var showDropdown by remember { mutableStateOf(false) }
                        val cardBg = Color(tracker.color)
                        val textColor = ColorUtils.getContrastingTextColor(cardBg)
                        val secondaryTextColor = ColorUtils.getSecondaryTextColor(cardBg)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale()
                                .combinedClickable(
                                    onClick = { navController.navigate("tracker_details/${tracker.id}") },
                                    onLongClick = { showDropdown = true }
                                ),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    // Row with Emoji and Title
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(textColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(tracker.emoji, style = MaterialTheme.typography.titleMedium)
                                        }

                                        IconButton(
                                            onClick = { navController.navigate("edit_tracker/${tracker.id}") },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Tracker",
                                                tint = textColor.copy(alpha = 0.8f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        tracker.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text(
                                        "${tracker.currentCount} / ${tracker.goal}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Progress Bar
                                    val progress = if (tracker.goal > 0) {
                                        (tracker.currentCount.toFloat() / tracker.goal.toFloat()).coerceIn(0f, 1f)
                                    } else {
                                        0f
                                    }
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = textColor,
                                        trackColor = textColor.copy(alpha = 0.2f)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    val haptic = LocalHapticFeedback.current

                                    // Quick Dual Stepper: Decrement (-) and Increment (+)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = textColor.copy(alpha = 0.16f)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.decrementTracker(tracker.id)
                                                    },
                                                    enabled = tracker.currentCount > 0,
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Remove,
                                                        contentDescription = "Decrement",
                                                        tint = if (tracker.currentCount > 0) textColor else textColor.copy(alpha = 0.35f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .width(1.dp)
                                                        .height(16.dp)
                                                        .background(textColor.copy(alpha = 0.25f))
                                                )

                                                IconButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewModel.incrementTracker(tracker.id)
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "Increment",
                                                        tint = textColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                DropdownMenu(
                                    expanded = showDropdown,
                                    onDismissRequest = { showDropdown = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Archive") },
                                        onClick = {
                                            viewModel.archiveTracker(tracker, true)
                                            showDropdown = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            showDeleteDialog = tracker
                                            showDropdown = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Tracker") },
            text = { Text("Are you sure you want to delete \"${showDeleteDialog?.title}\"? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog?.let { viewModel.deleteTracker(it) }
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
