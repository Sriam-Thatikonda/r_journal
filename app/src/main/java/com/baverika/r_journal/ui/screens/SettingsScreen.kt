package com.baverika.r_journal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.util.Calendar
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.baverika.r_journal.repository.PasswordRepository
import com.baverika.r_journal.repository.SettingsRepository
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.LightColors
import com.baverika.r_journal.ui.theme.MidnightColors
import com.baverika.r_journal.ui.theme.OceanColors
import com.baverika.r_journal.ui.theme.RosewoodColors
import com.baverika.r_journal.ui.theme.BlueSkyColors
import com.baverika.r_journal.ui.theme.CloudDancerColors
import com.baverika.r_journal.BuildConfig
import com.baverika.r_journal.widget.WidgetUpdateUtils
import kotlinx.coroutines.launch

import androidx.fragment.app.FragmentActivity
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.TaskRepository
import com.baverika.r_journal.utils.BiometricHelper
import com.baverika.r_journal.utils.MilestoneCalculator
import com.baverika.r_journal.ui.components.MilestoneCardDialog
import androidx.compose.material.icons.filled.EmojiEvents
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    passwordRepo: PasswordRepository? = null,
    journalRepo: JournalRepository,
    taskRepo: TaskRepository,
    navController: NavController,
    onThemeChanged: (AppTheme) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showMilestoneDialog by remember { mutableStateOf(false) }

    val entries by journalRepo.allEntries.collectAsState(initial = emptyList())
    val tasks by taskRepo.allTasks.collectAsState(initial = emptyList())

    val milestoneStats = remember(entries, tasks) {
        MilestoneCalculator.calculateCurrentYearStats(entries, tasks)
    }

    var isBiometricEnabled by remember { mutableStateOf(settingsRepo.isBiometricEnabled) }
    var currentTheme by remember { mutableStateOf(settingsRepo.appTheme) }
    var birthDay by remember { mutableIntStateOf(settingsRepo.birthDay) }
    var birthMonth by remember { mutableIntStateOf(settingsRepo.birthMonth) }
    var birthYear by remember { mutableIntStateOf(settingsRepo.birthYear) }
    var widgetOpacity by remember { mutableFloatStateOf(settingsRepo.widgetOpacity.toFloat()) }

    var notifTasks by remember { mutableStateOf(settingsRepo.notifTasksEnabled) }
    var notifTaskSummary by remember { mutableStateOf(settingsRepo.notifTaskSummaryEnabled) }
    var notifHabits by remember { mutableStateOf(settingsRepo.notifHabitsEnabled) }
    var notifJournal by remember { mutableStateOf(settingsRepo.notifJournalEnabled) }
    var notifEvents by remember { mutableStateOf(settingsRepo.notifEventsEnabled) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notification permission is required to receive reminders", Toast.LENGTH_SHORT).show()
        }
    }

    val requestNotifPermissionIfNeeded: () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Compose M3 DatePicker Logic
    var showDatePickerDialog by remember { mutableStateOf(false) }

    if (showDatePickerDialog) {
        val utcTz = TimeZone.getTimeZone("UTC")
        val initialCal = Calendar.getInstance(utcTz).apply {
            clear()
            set(birthYear, birthMonth - 1, birthDay)
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialCal.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val cal = Calendar.getInstance(utcTz).apply { timeInMillis = millis }
                            birthYear = cal.get(Calendar.YEAR)
                            birthMonth = cal.get(Calendar.MONTH) + 1
                            birthDay = cal.get(Calendar.DAY_OF_MONTH)

                            settingsRepo.birthYear = birthYear
                            settingsRepo.birthMonth = birthMonth
                            settingsRepo.birthDay = birthDay
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("OK")
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {


            // --- Appearance Section ---
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "Theme",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Theme Selection - Two rows for 6 themes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    name = "Midnight",
                    backgroundColor = MidnightColors.Background,
                    primaryColor = MidnightColors.Primary,
                    isSelected = currentTheme == AppTheme.MIDNIGHT,
                    isLightBackground = false,
                    onClick = {
                        currentTheme = AppTheme.MIDNIGHT
                        settingsRepo.appTheme = AppTheme.MIDNIGHT
                        onThemeChanged(AppTheme.MIDNIGHT)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    name = "Light",
                    backgroundColor = LightColors.Background,
                    primaryColor = LightColors.Primary,
                    isSelected = currentTheme == AppTheme.LIGHT,
                    isLightBackground = true,
                    onClick = {
                        currentTheme = AppTheme.LIGHT
                        settingsRepo.appTheme = AppTheme.LIGHT
                        onThemeChanged(AppTheme.LIGHT)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    name = "Ocean",
                    backgroundColor = OceanColors.Background,
                    primaryColor = OceanColors.Primary,
                    isSelected = currentTheme == AppTheme.OCEAN,
                    isLightBackground = false,
                    onClick = {
                        currentTheme = AppTheme.OCEAN
                        settingsRepo.appTheme = AppTheme.OCEAN
                        onThemeChanged(AppTheme.OCEAN)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeOption(
                    name = "Space",
                    backgroundColor = RosewoodColors.Background,
                    primaryColor = RosewoodColors.Primary,
                    isSelected = currentTheme == AppTheme.ROSEWOOD,
                    isLightBackground = false,
                    onClick = {
                        currentTheme = AppTheme.ROSEWOOD
                        settingsRepo.appTheme = AppTheme.ROSEWOOD
                        onThemeChanged(AppTheme.ROSEWOOD)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    name = "Blue Sky",
                    backgroundColor = BlueSkyColors.Background,
                    primaryColor = BlueSkyColors.Primary,
                    isSelected = currentTheme == AppTheme.BLUE_SKY,
                    isLightBackground = false,
                    onClick = {
                        currentTheme = AppTheme.BLUE_SKY
                        settingsRepo.appTheme = AppTheme.BLUE_SKY
                        onThemeChanged(AppTheme.BLUE_SKY)
                    },
                    modifier = Modifier.weight(1f)
                )
                ThemeOption(
                    name = "Cloud '26",
                    backgroundColor = CloudDancerColors.Background,
                    primaryColor = CloudDancerColors.Primary,
                    isSelected = currentTheme == AppTheme.CLOUD_DANCER,
                    isLightBackground = true,
                    onClick = {
                        currentTheme = AppTheme.CLOUD_DANCER
                        settingsRepo.appTheme = AppTheme.CLOUD_DANCER
                        onThemeChanged(AppTheme.CLOUD_DANCER)
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Security Section ---
            Text(
                text = "Security",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Biometric Lock",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Require fingerprint or face unlock",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { wantEnabled ->
                        val activity = context as? FragmentActivity
                        if (wantEnabled) {
                            if (BiometricHelper.isBiometricAvailable(context)) {
                                isBiometricEnabled = true
                                settingsRepo.isBiometricEnabled = true
                                MainActivity.updateSecureWindowFlag(activity, true)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Set up a screen lock or biometric on this device first",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            if (activity != null && BiometricHelper.isBiometricAvailable(context)) {
                                BiometricHelper.authenticate(
                                    activity = activity,
                                    title = "Disable Biometric Lock",
                                    subtitle = "Confirm your identity to turn off app lock",
                                    onSuccess = {
                                        isBiometricEnabled = false
                                        settingsRepo.isBiometricEnabled = false
                                        MainActivity.updateSecureWindowFlag(activity, false)
                                    },
                                    onError = {
                                        Toast.makeText(
                                            context,
                                            "Authentication required to disable Biometric Lock",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            } else {
                                isBiometricEnabled = false
                                settingsRepo.isBiometricEnabled = false
                                MainActivity.updateSecureWindowFlag(activity, false)
                            }
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Notifications & Reminders Section ---
            Text(
                text = "Notifications & Reminders",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Habit Evening Review
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Habit Evening Review", style = MaterialTheme.typography.bodyLarge)
                    Text("Nudges in the evening if scheduled habits are pending", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = notifHabits,
                    onCheckedChange = {
                        notifHabits = it
                        settingsRepo.notifHabitsEnabled = it
                        if (it) requestNotifPermissionIfNeeded()
                    }
                )
            }

            // Task Due Reminders
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Task Due Reminders", style = MaterialTheme.typography.bodyLarge)
                    Text("Alarms with Mark Done & Snooze action buttons", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = notifTasks,
                    onCheckedChange = {
                        notifTasks = it
                        settingsRepo.notifTasksEnabled = it
                        if (it) requestNotifPermissionIfNeeded()
                    }
                )
            }

            // Morning Task Briefing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Morning Task Briefing", style = MaterialTheme.typography.bodyLarge)
                    Text("Morning summary of tasks scheduled for today", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = notifTaskSummary,
                    onCheckedChange = {
                        notifTaskSummary = it
                        settingsRepo.notifTaskSummaryEnabled = it
                        if (it) requestNotifPermissionIfNeeded()
                    }
                )
            }

            // Daily Journal Prompt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Daily Journal Prompt", style = MaterialTheme.typography.bodyLarge)
                    Text("Evening reflection reminder (skips if already written)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = notifJournal,
                    onCheckedChange = {
                        notifJournal = it
                        settingsRepo.notifJournalEnabled = it
                        if (it) requestNotifPermissionIfNeeded()
                    }
                )
            }

            // Birthday & Special Events
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Birthday & Event Alerts", style = MaterialTheme.typography.bodyLarge)
                    Text("Morning alerts on birthdays and anniversaries", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = notifEvents,
                    onCheckedChange = {
                        notifEvents = it
                        settingsRepo.notifEventsEnabled = it
                        if (it) requestNotifPermissionIfNeeded()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Data Management Section ---
            Text(
                text = "Data Management & Backups",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            var backupDays by remember { mutableIntStateOf(settingsRepo.backupReminderDays) }

            if (settingsRepo.isBackupOverdue()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Backup Due",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "It has been over $backupDays days since your last backup.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Button(
                            onClick = {
                                navController.navigate("export")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Export")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto Backup Reminder", style = MaterialTheme.typography.bodyLarge)
                    Text("Sends notification when backup is due", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(7, 14, 30, 0).forEach { days ->
                        FilterChip(
                            selected = backupDays == days,
                            onClick = {
                                backupDays = days
                                settingsRepo.backupReminderDays = days
                                if (days > 0) requestNotifPermissionIfNeeded()
                            },
                            label = { Text(if (days == 0) "Off" else "${days}d") }
                        )
                    }
                }
            }

            SettingsItem(
                icon = Icons.Default.Upload,
                title = "Export Data",
                subtitle = "Backup journals, notes, tasks and settings",
                onClick = {
                    navController.navigate("export")
                }
            )

            SettingsItem(
                icon = Icons.Default.Download,
                title = "Import Data",
                subtitle = "Restore from JSON backup file",
                onClick = { navController.navigate("import") }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Widget Customization Section ---
            Text(
                text = "Widget Customization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Widget Background Opacity",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${widgetOpacity.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "Set background transparency for widgets",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Slider(
                    value = widgetOpacity,
                    onValueChange = {
                        widgetOpacity = it
                        settingsRepo.widgetOpacity = it.toInt()
                    },
                    valueRange = 0f..100f,
                    onValueChangeFinished = {
                        WidgetUpdateUtils.updateAllWidgets(context)
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Personalization Section ---
            Text(
                text = "Personalization",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Cake,
                title = "Birthday",
                subtitle = "$birthDay/${birthMonth}/$birthYear",
                onClick = { showDatePickerDialog = true }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Milestones & Analytics Section ---
            Text(
                text = "Milestones & Analytics",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.EmojiEvents,
                title = "${milestoneStats.year} Milestone Card",
                subtitle = "View your ${milestoneStats.year} story & personal statistics (Jan 1 – Dec 31)",
                onClick = { showMilestoneDialog = true }
            )

            if (showMilestoneDialog) {
                MilestoneCardDialog(
                    stats = milestoneStats,
                    onDismiss = { showMilestoneDialog = false }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
    }
}

@Composable
private fun ThemeOption(
    name: String,
    backgroundColor: Color,
    primaryColor: Color,
    isSelected: Boolean,
    isLightBackground: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(14.dp)
                ) else if (name == "Space") Modifier.border(
                    width = 1.dp,
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(14.dp)
                ) else Modifier.border(
                    width = 1.dp,
                    color = if (isLightBackground) Color(0x22000000) else Color(0x22FFFFFF),
                    shape = RoundedCornerShape(14.dp)
                )
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = if (isSelected) 4.dp else 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // Mock screen preview elements
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Mini mock header bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 24.dp, height = 4.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                }

                // Mini mock card
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isLightBackground) Color(0x18000000) else Color(0x22FFFFFF)
                        )
                )
            }

            // Theme name & Selection badge at bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLightBackground)
                        Color(0xFF2C2520)
                    else
                        Color.White.copy(alpha = 0.95f)
                )

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = if (isLightBackground) Color.White else Color.Black,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
    }
}
