package com.baverika.r_journal.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskCategory
import com.baverika.r_journal.data.local.entity.TaskPriority
import com.baverika.r_journal.ui.viewmodel.TaskFilter
import com.baverika.r_journal.ui.viewmodel.TaskSortOption
import com.baverika.r_journal.ui.viewmodel.TaskStats
import com.baverika.r_journal.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Main Task List Screen with comprehensive task management UI.
 * 
 * Features:
 * - Task list with swipe actions
 * - Filtering by status, category, and priority
 * - Sorting options
 * - Search functionality
 * - Task statistics summary
 * - Empty state handling
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    navController: NavController
) {
    val tasks by viewModel.tasks.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val taskStats by viewModel.taskStats.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    val selectedPriority by viewModel.selectedPriority.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show error message
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            if (isSearchActive) {
                // Search TopBar
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    onClose = {
                        viewModel.setSearchQuery("")
                        isSearchActive = false
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Tasks") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        // Search button
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        
                        // Sort button
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort")
                            }
                            SortDropdownMenu(
                                expanded = showSortMenu,
                                currentSort = currentSort,
                                onSortSelected = {
                                    viewModel.setSort(it)
                                    showSortMenu = false
                                },
                                onDismiss = { showSortMenu = false }
                            )
                        }
                        
                        // Filter button
                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (selectedCategoryId != null || selectedPriority != null) {
                                        Badge { Text("!") }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filter")
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("add_task") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Task") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Statistics Summary Card
            TaskStatsSummary(stats = taskStats)
            
            // Filter Chips
            FilterChipsRow(
                currentFilter = currentFilter,
                selectedCategoryId = selectedCategoryId,
                selectedPriority = selectedPriority,
                categories = categories,
                onFilterSelected = { viewModel.setFilter(it) },
                onClearFilters = { viewModel.clearFilters() }
            )
            
            // Task List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (tasks.isEmpty()) {
                EmptyTasksState(
                    filter = currentFilter,
                    searchQuery = searchQuery,
                    onAddTask = { navController.navigate("add_task") }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = tasks,
                        key = { it.id }
                    ) { task ->
                        SwipeableTaskItem(
                            task = task,
                            category = categories.find { it.id == task.categoryId },
                            onToggleComplete = { viewModel.toggleTaskCompletion(task.id, !task.isCompleted) },
                            onClick = { navController.navigate("edit_task/${task.id}") },
                            onDelete = { taskToDelete = task }
                        )
                    }
                }
            }
        }
    }
    
    // Filter Bottom Sheet
    if (showFilterSheet) {
        FilterBottomSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            selectedPriority = selectedPriority,
            onCategorySelected = { viewModel.filterByCategory(it) },
            onPrioritySelected = { viewModel.filterByPriority(it) },
            onDismiss = { showFilterSheet = false }
        )
    }
    
    // Delete Confirmation Dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTask(task)
                        taskToDelete = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Task deleted")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Search-enabled top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit
) {
    TopAppBar(
        title = {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search tasks...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close search")
            }
        },
        actions = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

/**
 * Sort dropdown menu.
 */
@Composable
private fun SortDropdownMenu(
    expanded: Boolean,
    currentSort: TaskSortOption,
    onSortSelected: (TaskSortOption) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        TaskSortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = getSortIcon(option),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(getSortDisplayName(option))
                    }
                },
                onClick = { onSortSelected(option) },
                leadingIcon = if (currentSort == option) {
                    { Icon(Icons.Default.Check, contentDescription = null) }
                } else null
            )
        }
    }
}

private fun getSortIcon(option: TaskSortOption): ImageVector {
    return when (option) {
        TaskSortOption.DUE_DATE -> Icons.Default.Schedule
        TaskSortOption.PRIORITY -> Icons.Default.Flag
        TaskSortOption.CREATED_DATE -> Icons.Default.CalendarToday
        TaskSortOption.ALPHABETICAL -> Icons.Default.SortByAlpha
    }
}

private fun getSortDisplayName(option: TaskSortOption): String {
    return when (option) {
        TaskSortOption.DUE_DATE -> "Due Date"
        TaskSortOption.PRIORITY -> "Priority"
        TaskSortOption.CREATED_DATE -> "Created Date"
        TaskSortOption.ALPHABETICAL -> "Alphabetical"
    }
}

/**
 * Task statistics summary card.
 */
@Composable
private fun TaskStatsSummary(stats: TaskStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                value = stats.activeTasks.toString(),
                label = "Active",
                color = MaterialTheme.colorScheme.primary
            )
            StatItem(
                value = stats.completedToday.toString(),
                label = "Today",
                color = Color(0xFF66BB6A)
            )
            StatItem(
                value = stats.overdueTasks.toString(),
                label = "Overdue",
                color = if (stats.overdueTasks > 0) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatItem(
                value = stats.completedTasks.toString(),
                label = "Done",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Filter chips row.
 */
@Composable
private fun FilterChipsRow(
    currentFilter: TaskFilter,
    selectedCategoryId: String?,
    selectedPriority: TaskPriority?,
    categories: List<TaskCategory>,
    onFilterSelected: (TaskFilter) -> Unit,
    onClearFilters: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Clear filters chip (only show when filters are active)
        if (selectedCategoryId != null || selectedPriority != null) {
            item {
                AssistChip(
                    onClick = onClearFilters,
                    label = { Text("Clear") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
        
        // Filter chips
        TaskFilter.entries.forEach { filter ->
            item {
                FilterChip(
                    selected = currentFilter == filter && selectedCategoryId == null && selectedPriority == null,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(getFilterDisplayName(filter)) },
                    leadingIcon = if (currentFilter == filter && selectedCategoryId == null && selectedPriority == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
    }
}

private fun getFilterDisplayName(filter: TaskFilter): String {
    return when (filter) {
        TaskFilter.ALL -> "All"
        TaskFilter.ACTIVE -> "Active"
        TaskFilter.COMPLETED -> "Completed"
        TaskFilter.OVERDUE -> "Overdue"
        TaskFilter.TODAY -> "Today"
    }
}

/**
 * Swipeable task item with completion toggle and delete action.
 */
@Composable
fun SwipeableTaskItem(
    task: Task,
    category: TaskCategory?,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = with(LocalDensity.current) { 100.dp.toPx() }
    
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "swipe_offset"
    )
    
    // Completion animation removed to prevent background bleed-through

    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Background actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(16.dp))
                .graphicsLayer { 
                    alpha = (kotlin.math.abs(animatedOffset) / (swipeThreshold * 0.5f)).coerceIn(0f, 1f)
                },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left side - Complete action
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFF66BB6A))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Complete",
                    tint = Color.White
                )
            }
            
            // Right side - Delete action
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(Color(0xFFE53935))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
        
        // Task card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX > swipeThreshold -> {
                                    onToggleComplete()
                                }
                                offsetX < -swipeThreshold -> {
                                    onDelete()
                                }
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount).coerceIn(-swipeThreshold * 1.5f, swipeThreshold * 1.5f)
                    }
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (task.isCompleted) 0.dp else 2.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Completion checkbox
                TaskCheckbox(
                    isCompleted = task.isCompleted,
                    priority = task.priority,
                    onToggle = onToggleComplete
                )
                
                // Task content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Title
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Description preview
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    // Meta info row
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Due date
                        task.dueDate?.let { dueDate ->
                            val isOverdue = dueDate < System.currentTimeMillis() && !task.isCompleted
                            DueDateChip(
                                dueDate = dueDate,
                                isOverdue = isOverdue
                            )
                        }
                        
                        // Category
                        category?.let {
                            CategoryChip(category = it)
                        }
                    }
                }
                
                // Priority indicator
                PriorityIndicator(priority = task.priority)
            }
        }
    }
}

/**
 * Custom checkbox with priority-colored border.
 */
@Composable
private fun TaskCheckbox(
    isCompleted: Boolean,
    priority: TaskPriority,
    onToggle: () -> Unit
) {
    val priorityColor = Color(priority.colorValue)
    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "check_scale"
    )
    
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (isCompleted) priorityColor else Color.Transparent
            )
            .border(
                width = 2.dp,
                color = priorityColor,
                shape = CircleShape
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .scale(checkScale)
        )
    }
}

/**
 * Priority indicator badge.
 */
@Composable
private fun PriorityIndicator(priority: TaskPriority) {
    val color = Color(priority.colorValue)
    
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Flag,
            contentDescription = priority.displayName,
            tint = color,
            modifier = Modifier
                .padding(4.dp)
                .size(16.dp)
        )
    }
}

/**
 * Due date chip.
 */
@Composable
private fun DueDateChip(
    dueDate: Long,
    isOverdue: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    val formattedDate = remember(dueDate) {
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        calendar.timeInMillis = dueDate
        
        when {
            isSameDay(calendar, today) -> "Today, ${timeFormat.format(Date(dueDate))}"
            isSameDay(calendar, today.apply { add(Calendar.DAY_OF_YEAR, 1) }) -> "Tomorrow"
            else -> dateFormat.format(Date(dueDate))
        }
    }
    
    Surface(
        color = if (isOverdue) {
            Color(0xFFE53935).copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        },
        shape = RoundedCornerShape(4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOverdue) Icons.Default.Warning else Icons.Outlined.Schedule,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = if (isOverdue) Color(0xFFE53935) else MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall,
                color = if (isOverdue) Color(0xFFE53935) else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * Category chip.
 */
@Composable
private fun CategoryChip(category: TaskCategory) {
    Surface(
        color = Color(category.color).copy(alpha = 0.15f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelSmall,
            color = Color(category.color),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Empty state for task list.
 */
@Composable
private fun EmptyTasksState(
    filter: TaskFilter,
    searchQuery: String,
    onAddTask: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = when {
                searchQuery.isNotBlank() -> Icons.Outlined.SearchOff
                filter == TaskFilter.COMPLETED -> Icons.Outlined.CheckCircle
                filter == TaskFilter.OVERDUE -> Icons.Outlined.Schedule
                else -> Icons.Outlined.Task
            },
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when {
                searchQuery.isNotBlank() -> "No tasks found"
                filter == TaskFilter.COMPLETED -> "No completed tasks yet"
                filter == TaskFilter.OVERDUE -> "No overdue tasks"
                filter == TaskFilter.TODAY -> "No tasks due today"
                else -> "No tasks yet"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = when {
                searchQuery.isNotBlank() -> "Try a different search term"
                filter == TaskFilter.COMPLETED -> "Complete some tasks to see them here"
                else -> "Add your first task to get started"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        
        if (filter == TaskFilter.ALL || filter == TaskFilter.ACTIVE) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(onClick = onAddTask) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Task")
            }
        }
    }
}

/**
 * Filter bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    categories: List<TaskCategory>,
    selectedCategoryId: String?,
    selectedPriority: TaskPriority?,
    onCategorySelected: (String?) -> Unit,
    onPrioritySelected: (TaskPriority?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Filter by",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Priority section
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskPriority.entries.forEach { priority ->
                    FilterChip(
                        selected = selectedPriority == priority,
                        onClick = {
                            onPrioritySelected(if (selectedPriority == priority) null else priority)
                        },
                        label = { Text(priority.displayName) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(priority.colorValue))
                            )
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category section
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = {
                            onCategorySelected(if (selectedCategoryId == category.id) null else category.id)
                        },
                        label = { Text(category.name) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.color))
                            )
                        }
                    )
                }
            }
        }
    }
}
