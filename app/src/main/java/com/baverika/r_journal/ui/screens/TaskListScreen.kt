package com.baverika.r_journal.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import com.baverika.r_journal.ui.components.HighlightedText
import com.baverika.r_journal.ui.theme.JetBrainsMonoFamily
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import com.baverika.r_journal.ui.theme.pressScale
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
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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

    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            searchFocusRequester.requestFocus()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val tokens = LocalDesignTokens.current
            val haptic = LocalHapticFeedback.current

            // Consolidated Smart Header & Quick Controls
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(tokens.cardRadius),
                color = tokens.cardContainer,
                border = tokens.strokeSubtle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val total = taskStats.totalTasks
                    val completed = taskStats.completedTasks
                    val targetProgress = if (total > 0) completed.toFloat() / total.toFloat() else 0f
                    val animatedProgress by animateFloatAsState(
                        targetValue = targetProgress,
                        animationSpec = tween(400),
                        label = "taskProgress"
                    )
                    val percentage = (animatedProgress * 100).toInt()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 3.5.dp
                            )
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = JetBrainsMonoFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column {
                            Text(
                                text = "$completed of $total Tasks Done",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${taskStats.activeTasks} active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (taskStats.overdueTasks > 0) {
                                    Text(
                                        text = "• ${taskStats.overdueTasks} overdue",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFE53935)
                                    )
                                }
                            }
                        }
                    }

                    // Right Controls: Sort & Filter
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = getSortIcon(currentSort),
                                    contentDescription = "Sort tasks",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
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

                        IconButton(onClick = { showFilterSheet = true }) {
                            BadgedBox(
                                badge = {
                                    if (selectedCategoryId != null || selectedPriority != null) {
                                        Badge { Text("!") }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filter tasks",
                                    tint = if (selectedCategoryId != null || selectedPriority != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            val orderedFilters = remember {
                listOf(
                    TaskFilter.ALL,
                    TaskFilter.TODAY,
                    TaskFilter.ACTIVE,
                    TaskFilter.OVERDUE,
                    TaskFilter.COMPLETED
                )
            }

            // Smart Segment Tabs Row (All, Today, Active, Overdue, Completed)
            ScrollableTabRow(
                selectedTabIndex = orderedFilters.indexOf(currentFilter).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp,
                divider = {},
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                orderedFilters.forEach { filter ->
                    val isSelected = currentFilter == filter
                    val count = when (filter) {
                        TaskFilter.ALL -> taskStats.totalTasks
                        TaskFilter.TODAY -> taskStats.completedToday
                        TaskFilter.ACTIVE -> taskStats.activeTasks
                        TaskFilter.OVERDUE -> taskStats.overdueTasks
                        TaskFilter.COMPLETED -> taskStats.completedTasks
                    }

                    Tab(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setFilter(filter)
                        },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = getFilterDisplayName(filter),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                        color = if (filter == TaskFilter.OVERDUE && count > 0) {
                                            Color(0xFFE53935)
                                        } else if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // Active Category / Priority Filters Row (if filtered)
            if (selectedCategoryId != null || selectedPriority != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    selectedCategoryId?.let { catId ->
                        val cat = categories.find { it.id == catId }
                        if (cat != null) {
                            InputChip(
                                selected = true,
                                onClick = { viewModel.filterByCategory(null) },
                                label = { Text(cat.name) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear category", modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }

                    selectedPriority?.let { pri ->
                        InputChip(
                            selected = true,
                            onClick = { viewModel.filterByPriority(null) },
                            label = { Text(pri.displayName) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear priority", modifier = Modifier.size(14.dp)) }
                        )
                    }

                    TextButton(
                        onClick = { viewModel.clearFilters() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

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
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = tasks,
                        key = { it.id }
                    ) { task ->
                        SwipeableTaskItem(
                            task = task,
                            category = categories.find { it.id == task.categoryId },
                            searchQuery = searchQuery,
                            onToggleComplete = {
                                viewModel.toggleTaskCompletion(
                                    task.id,
                                    !task.isCompleted
                                )
                            },
                            onClick = { navController.navigate("edit_task/${task.id}") },
                            onDelete = { taskToDelete = task }
                        )
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
    val tokens = LocalDesignTokens.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = tokens.cardContainer
        ),
        border = tokens.strokeSubtle,
        shape = RoundedCornerShape(tokens.cardRadius)
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
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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
    searchQuery: String = "",
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
                    alpha =
                        (kotlin.math.abs(animatedOffset) / (swipeThreshold * 0.5f)).coerceIn(
                            0f,
                            1f
                        )
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
        val tokens = LocalDesignTokens.current
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pressScale()
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
                        offsetX = (offsetX + dragAmount).coerceIn(
                            -swipeThreshold * 1.5f,
                            swipeThreshold * 1.5f
                        )
                    }
                }
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(tokens.cardRadius),
            colors = CardDefaults.cardColors(
                containerColor = if (task.isCompleted) {
                    tokens.cardContainer.copy(alpha = 0.65f)
                } else {
                    tokens.cardContainer
                }
            ),
            border = tokens.strokeSubtle,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    HighlightedText(
                        text = task.title,
                        query = searchQuery,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        highlightColor = MaterialTheme.colorScheme.primary,
                        highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Description preview
                    if (task.description.isNotBlank()) {
                        HighlightedText(
                            text = task.description,
                            query = searchQuery,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            highlightColor = MaterialTheme.colorScheme.primary,
                            highlightBackground = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
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
                            val isOverdue =
                                dueDate < System.currentTimeMillis() && !task.isCompleted
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
 * Custom checkbox with priority-colored border and bouncy animation.
 */
@Composable
private fun TaskCheckbox(
    isCompleted: Boolean,
    priority: TaskPriority,
    onToggle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val priorityColor = Color(priority.colorValue)
    val checkScale by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "check_scale"
    )

    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggle()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) priorityColor else Color.Transparent
                )
                .border(
                    width = 2.dp,
                    color = priorityColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(15.dp)
                        .scale(checkScale)
                )
            }
        }
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
 * Due date chip with chromatic urgency.
 */
@Composable
private fun DueDateChip(
    dueDate: Long,
    isOverdue: Boolean
) {
    val dateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    val calendar = remember(dueDate) {
        Calendar.getInstance().apply { timeInMillis = dueDate }
    }
    val today = remember { Calendar.getInstance() }
    val tomorrow = remember { Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) } }

    val isToday = isSameDay(calendar, today)
    val isTomorrow = isSameDay(calendar, tomorrow)

    val labelText = when {
        isOverdue && isToday -> "Overdue • Today, ${timeFormat.format(Date(dueDate))}"
        isOverdue -> "Overdue • ${dateFormat.format(Date(dueDate))}"
        isToday -> "Today, ${timeFormat.format(Date(dueDate))}"
        isTomorrow -> "Tomorrow"
        else -> dateFormat.format(Date(dueDate))
    }

    val (containerColor, contentColor, icon) = when {
        isOverdue -> Triple(
            Color(0xFFE53935).copy(alpha = 0.16f),
            Color(0xFFE53935),
            Icons.Default.Warning
        )
        isToday -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Outlined.Schedule
        )
        isTomorrow -> Triple(
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Outlined.Schedule
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Outlined.CalendarToday
        )
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = contentColor
            )
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isOverdue || isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
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
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { category ->
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

