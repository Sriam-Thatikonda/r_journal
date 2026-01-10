package com.baverika.r_journal.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.baverika.r_journal.data.local.entity.LifeTracker
import com.baverika.r_journal.ui.viewmodel.LifeTrackerViewModel
import com.baverika.r_journal.ui.viewmodel.TrackerUiModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeTrackersScreen(
    viewModel: LifeTrackerViewModel,
    onTrackerClick: (String) -> Unit
) {
    val trackers by viewModel.trackers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var trackerToEdit by remember { mutableStateOf<LifeTracker?>(null) }
    var trackerToDelete by remember { mutableStateOf<LifeTracker?>(null) }
    
    var isGridView by remember { mutableStateOf(true) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val focusManager = LocalFocusManager.current

    val filteredTrackers = remember(trackers, searchQuery) {
        if (searchQuery.isBlank()) trackers else trackers.filter { 
            it.tracker.name.contains(searchQuery, ignoreCase = true) 
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search trackers...") },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Life Trackers", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        if (isSearchActive) {
                             isSearchActive = false
                             searchQuery = ""
                        } else {
                             isSearchActive = true
                        }
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (isSearchActive) "Close Search" else "Search"
                        )
                    }
                    if (!isSearchActive) {
                        IconButton(onClick = { isGridView = !isGridView }) {
                            Icon(
                                if (isGridView) Icons.Default.List else Icons.Default.GridView,
                                contentDescription = "Toggle View"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Tracker")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (trackers.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No trackers yet. Add one!", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (filteredTrackers.isEmpty()) {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No matching trackers found.", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                    ) {
                        items(filteredTrackers) { item ->
                            TrackerGridCard(
                                item = item, 
                                onClick = { onTrackerClick(item.tracker.id) },
                                onEdit = { trackerToEdit = item.tracker },
                                onDelete = { trackerToDelete = item.tracker }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)
                    ) {
                        items(filteredTrackers) { item ->
                            TrackerListCard(
                                item = item, 
                                onClick = { onTrackerClick(item.tracker.id) },
                                onEdit = { trackerToEdit = item.tracker },
                                onDelete = { trackerToDelete = item.tracker }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        TrackerDialog(
            tracker = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, icon, color ->
                viewModel.createTracker(name, icon, color)
                showAddDialog = false
            }
        )
    }

    if (trackerToEdit != null) {
        TrackerDialog(
            tracker = trackerToEdit,
            onDismiss = { trackerToEdit = null },
            onSave = { name, icon, color ->
                // Create new object with existing ID
                val updatedTracker = trackerToEdit!!.copy(
                    name = name,
                    icon = icon,
                    color = color
                )
                viewModel.updateTracker(updatedTracker)
                trackerToEdit = null
            }
        )
    }

    if (trackerToDelete != null) {
        DeleteTrackerDialog(
            onDismiss = { trackerToDelete = null },
            onConfirm = {
                viewModel.deleteTracker(trackerToDelete!!)
                trackerToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackerGridCard(
    item: TrackerUiModel, 
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(item.tracker.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.tracker.icon, style = MaterialTheme.typography.titleLarge)
                }
                
                Column {
                    Text(
                        item.tracker.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (item.lastEntry != null) {
                        Text(
                            formatDate(item.lastEntry.dateMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (item.lastEntry.note.isNotBlank()) {
                             Text(
                                item.lastEntry.note,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text("No entries", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { showMenu = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { showMenu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackerListCard(
    item: TrackerUiModel, 
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(item.tracker.color).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(item.tracker.icon, style = MaterialTheme.typography.headlineSmall)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                     Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                         Text(
                            item.tracker.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                         if (item.lastEntry != null) {
                            Text(
                                formatDate(item.lastEntry.dateMillis),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                     }
                     if (item.lastEntry != null && item.lastEntry.note.isNotBlank()) {
                         Text(
                            item.lastEntry.note,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                     } else if (item.lastEntry == null) {
                         Text("No entries yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                     }
                }
            }
            
             DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit") },
                    onClick = { showMenu = false; onEdit() },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { showMenu = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDialog(
    tracker: LifeTracker?,
    onDismiss: () -> Unit, 
    onSave: (String, String, Long) -> Unit
) {
    var name by remember { mutableStateOf(tracker?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(tracker?.icon ?: "\uD83D\uDCC5") } // Default Calendar
    var selectedColor by remember { mutableStateOf(tracker?.color ?: 0xFF2196F3) } // Default Blue
    
    val icons = listOf("⚽", "\uD83D\uDE97", "\uD83D\uDCBB", "\uD83D\uDCAA", "\uD83E\uDE7A", "\uD83D\uDCD6", "\uD83C\uDFA8", "\uD83C\uDFB8", "✂️", "\uD83D\uDEEC")
    val colors = listOf(0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 0xFF2196F3, 0xFF009688, 0xFF4CAF50, 0xFFFFC107, 0xFFFF5722)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (tracker == null) "New Tracker" else "Edit Tracker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true
                )
                
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                   icons.take(5).forEach { icon ->
                       SelectionChip(selected = selectedIcon == icon, onClick = { selectedIcon = icon }, label = icon)
                   }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                   icons.drop(5).forEach { icon ->
                       SelectionChip(selected = selectedIcon == icon, onClick = { selectedIcon = icon }, label = icon)
                   }
                }

                Text("Color", style = MaterialTheme.typography.labelLarge)
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    colors.take(5).forEach { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(colorHex))
                                .clickable { selectedColor = colorHex }
                                .then(if (selectedColor == colorHex) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onSave(name, selectedIcon, selectedColor) },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DeleteTrackerDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Tracker") },
        text = { Text("Are you sure you want to delete this tracker? All entries will be lost.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = null
    )
}

private fun formatDate(millis: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return formatter.format(Date(millis))
}
