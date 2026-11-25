package com.baverika.r_journal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.data.local.entity.Task
import com.baverika.r_journal.data.local.entity.TaskCategory
import com.baverika.r_journal.data.local.entity.TaskPriority
import com.baverika.r_journal.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen for adding or editing a task.
 * 
 * Features:
 * - Title and description input
 * - Date and time picker for due date
 * - Priority selector
 * - Category selector with option to create new
 * - Input validation
 * - Save and cancel actions
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTaskScreen(
    viewModel: TaskViewModel,
    navController: NavController,
    taskId: String? = null
) {
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean -> }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    val categories by viewModel.categories.collectAsState()
    
    // State for form fields
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf<Long?>(null) }
    var dueTime by remember { mutableStateOf<Pair<Int, Int>?>(null) } // hour, minute
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var hasReminder by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<Long?>(null) }
    var selectedReminderOffset by remember { mutableStateOf<Long?>(null) }
    var loadedTask by remember { mutableStateOf<Task?>(null) }
    
    var isEditing by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var titleError by remember { mutableStateOf<String?>(null) }
    
    // For category creation dialog
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    
    // Load task data if editing
    LaunchedEffect(taskId) {
        if (taskId != null) {
            isEditing = true
            isLoading = true
            viewModel.getTaskById(taskId)?.let { task ->
                loadedTask = task
                title = task.title
                description = task.description
                dueDate = task.dueDate
                task.dueDate?.let {
                    val calendar = Calendar.getInstance().apply { timeInMillis = it }
                    dueTime = calendar.get(Calendar.HOUR_OF_DAY) to calendar.get(Calendar.MINUTE)
                }
                priority = task.priority
                selectedCategoryId = task.categoryId
                reminderTime = task.reminderTime
                hasReminder = task.reminderTime != null
                if (task.dueDate != null && task.reminderTime != null) {
                    val diff = task.dueDate - task.reminderTime
                    val presets = listOf(30L * 60 * 1000, 60L * 60 * 1000, 24L * 60 * 60 * 1000)
                    if (diff in presets) {
                        selectedReminderOffset = diff
                    }
                }
            }
            isLoading = false
        }
    }

    // Observe UI events
    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is com.baverika.r_journal.ui.viewmodel.TaskViewModel.TaskUiEvent.NavigateBack -> {
                    if (!navController.popBackStack()) {
                        navController.navigate("tasks") {
                            popUpTo(0)
                        }
                    }
                }
            }
        }
    }
    
    // Date format for display
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    
    // Calculate full due date/time in millis
    val fullDueDateTime: Long? = remember(dueDate, dueTime) {
        if (dueDate == null) return@remember null
        val calendar = Calendar.getInstance().apply {
            timeInMillis = dueDate!!
            dueTime?.let { (hour, minute) ->
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            } ?: run {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
        }
        calendar.timeInMillis
    }
    
    fun validateAndSave() {
        // Validate title
        if (title.isBlank()) {
            titleError = "Title is required"
            return
        }
        titleError = null

        val computedReminderTime: Long? = when {
            !hasReminder || fullDueDateTime == null -> null
            selectedReminderOffset != null -> fullDueDateTime - selectedReminderOffset!!
            else -> reminderTime
        }

        val now = System.currentTimeMillis()
        val baseTask = loadedTask
        val taskToSave = if (isEditing && baseTask != null) {
            baseTask.copy(
                title = title.trim(),
                description = description.trim(),
                dueDate = fullDueDateTime,
                priority = priority,
                categoryId = selectedCategoryId,
                reminderTime = computedReminderTime,
                updatedAt = now
            )
        } else {
            Task(
                id = taskId ?: UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                dueDate = fullDueDateTime,
                priority = priority,
                categoryId = selectedCategoryId,
                reminderTime = computedReminderTime,
                createdAt = now,
                updatedAt = now
            )
        }
        
        if (isEditing) {
            viewModel.updateTask(taskToSave)
        } else {
            viewModel.addTask(
                title = taskToSave.title,
                description = taskToSave.description,
                dueDate = taskToSave.dueDate,
                priority = taskToSave.priority,
                categoryId = taskToSave.categoryId,
                reminderTime = taskToSave.reminderTime
            )
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                    
                    Text(
                        text = if (isEditing) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )

                    if (isEditing) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    TextButton(
                        onClick = { validateAndSave() },
                        enabled = title.isNotBlank()
                    ) {
                        Text(
                            "Save",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleError = null
                    },
                    label = { Text("Task title *") },
                    placeholder = { Text("What needs to be done?") },
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    placeholder = { Text("Add notes or details...") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Due Date Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Due Date & Time",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Quick Presets Row: Today, Tomorrow, This Weekend, Next Week
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    dueDate = cal.timeInMillis
                                },
                                label = { Text("Today") },
                                leadingIcon = { Icon(Icons.Outlined.Today, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                                    dueDate = cal.timeInMillis
                                },
                                label = { Text("Tomorrow") },
                                leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            AssistChip(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    val daysUntilSaturday = (Calendar.SATURDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                                    cal.add(Calendar.DAY_OF_YEAR, if (daysUntilSaturday == 0) 7 else daysUntilSaturday)
                                    dueDate = cal.timeInMillis
                                },
                                label = { Text("This Weekend") }
                            )
                            AssistChip(
                                onClick = {
                                    val cal = Calendar.getInstance()
                                    val daysUntilMonday = (Calendar.MONDAY - cal.get(Calendar.DAY_OF_WEEK) + 7) % 7
                                    cal.add(Calendar.DAY_OF_YEAR, if (daysUntilMonday == 0) 7 else daysUntilMonday)
                                    dueDate = cal.timeInMillis
                                },
                                label = { Text("Next Week") }
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Date picker button (triggers M3 DatePicker)
                            DatePickerButton(
                                date = dueDate,
                                dateFormat = dateFormat,
                                onClick = { showDatePickerDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Time picker button (triggers M3 TimePicker)
                            TimePickerButton(
                                time = dueTime,
                                timeFormat = timeFormat,
                                enabled = dueDate != null,
                                onClick = { showTimePickerDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Clear date button
                        if (dueDate != null) {
                            TextButton(
                                onClick = {
                                    dueDate = null
                                    dueTime = null
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear due date")
                            }
                        }
                    }
                }
                
                // Priority Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Priority",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TaskPriority.entries.forEach { p ->
                                PriorityChip(
                                    priority = p,
                                    isSelected = priority == p,
                                    onClick = { priority = p },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Reminder Section (optional - placed above Category)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Reminder",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Get notified before due",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                            
                            Switch(
                                checked = hasReminder,
                                onCheckedChange = { isEnabled ->
                                    hasReminder = isEnabled
                                    if (isEnabled && selectedReminderOffset == null && reminderTime == null && fullDueDateTime != null) {
                                        val defaultOffset = 30L * 60 * 1000
                                        selectedReminderOffset = defaultOffset
                                        reminderTime = fullDueDateTime - defaultOffset
                                    }
                                    if (isEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.POST_NOTIFICATIONS
                                            ) != PackageManager.PERMISSION_GRANTED
                                        ) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                },
                                enabled = dueDate != null
                            )
                        }
                        
                        AnimatedVisibility(visible = hasReminder && dueDate != null) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Remind me:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "30 min" to 30L * 60 * 1000,
                                        "1 hour" to 60L * 60 * 1000,
                                        "1 day" to 24L * 60 * 60 * 1000
                                    ).forEach { (label, offset) ->
                                        val dueMillis = fullDueDateTime ?: 0L
                                        val reminderMillis = dueMillis - offset
                                        FilterChip(
                                            selected = selectedReminderOffset == offset || (selectedReminderOffset == null && reminderTime == reminderMillis),
                                            onClick = {
                                                selectedReminderOffset = offset
                                                reminderTime = reminderMillis
                                            },
                                            label = { Text(label) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Category Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            TextButton(
                                onClick = { showCategoryDialog = true },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New")
                            }
                        }
                        
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // No category option
                            CategoryChipSelectable(
                                name = "None",
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                isSelected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null }
                            )
                            
                            categories.forEach { category ->
                                CategoryChipSelectable(
                                    name = category.name,
                                    color = Color(category.color),
                                    isSelected = selectedCategoryId == category.id,
                                    onClick = { selectedCategoryId = category.id }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
    
    // Category Creation Dialog
    if (showCategoryDialog) {
        CategoryCreationDialog(
            onDismiss = { showCategoryDialog = false },
            onCategoryCreated = { name, color ->
                viewModel.addCategory(name, color)
                showCategoryDialog = false
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmation && taskId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Task") },
            text = { Text("Are you sure you want to delete this task?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTaskById(taskId)
                        showDeleteConfirmation = false
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Material 3 DatePickerDialog
    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { sel ->
                            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = sel }
                            val localCal = Calendar.getInstance().apply {
                                set(utcCal.get(Calendar.YEAR), utcCal.get(Calendar.MONTH), utcCal.get(Calendar.DAY_OF_MONTH))
                            }
                            dueDate = localCal.timeInMillis
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 TimePickerDialog (AlertDialog with TimePicker)
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = dueTime?.first ?: 12,
            initialMinute = dueTime?.second ?: 0,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueTime = timePickerState.hour to timePickerState.minute
                        showTimePickerDialog = false
                    }
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Set Time", fontWeight = FontWeight.Bold) },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }
}

/**
 * Date picker button.
 */
@Composable
private fun DatePickerButton(
    date: Long?,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = date?.let { dateFormat.format(Date(it)) } ?: "Set date",
                color = if (date != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Time picker button.
 */
@Composable
private fun TimePickerButton(
    time: Pair<Int, Int>?,
    timeFormat: SimpleDateFormat,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = { if (enabled) onClick() },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        },
        tonalElevation = if (enabled) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Schedule,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                }
            )
            Text(
                text = time?.let {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, it.first)
                        set(Calendar.MINUTE, it.second)
                    }
                    timeFormat.format(cal.time)
                } ?: "Set time",
                color = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    time != null -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Priority selection chip.
 */
@Composable
private fun PriorityChip(
    priority: TaskPriority,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = Color(priority.colorValue)
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, color, RoundedCornerShape(12.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = priority.displayName,
                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Category selection chip.
 */
@Composable
private fun CategoryChipSelectable(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(name) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    )
}

/**
 * Dialog for creating a new category.
 */
@Composable
private fun CategoryCreationDialog(
    onDismiss: () -> Unit,
    onCategoryCreated: (String, Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableIntStateOf(0xFF2196F3.toInt()) }
    
    val colorOptions = listOf(
        0xFF2196F3.toInt(), // Blue
        0xFF4CAF50.toInt(), // Green
        0xFFFF9800.toInt(), // Orange
        0xFFE91E63.toInt(), // Pink
        0xFF9C27B0.toInt(), // Purple
        0xFF00BCD4.toInt(), // Cyan
        0xFFFF5722.toInt(), // Deep Orange
        0xFF607D8B.toInt()  // Blue Grey
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Text(
                    text = "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.take(4).forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorOptions.drop(4).forEach { color ->
                        ColorCircle(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCategoryCreated(name, selectedColor) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Color selection circle.
 */
@Composable
private fun ColorCircle(
    color: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(color))
            .then(
                if (isSelected) {
                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
