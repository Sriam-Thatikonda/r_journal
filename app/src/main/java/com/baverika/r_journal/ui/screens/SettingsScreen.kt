package com.baverika.r_journal.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import com.baverika.r_journal.BuildConfig
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.PasswordRepository
import com.baverika.r_journal.repository.SettingsRepository
import com.baverika.r_journal.repository.TaskRepository
import com.baverika.r_journal.ui.components.MilestoneCardDialog
import com.baverika.r_journal.ui.components.PremiumCard
import com.baverika.r_journal.ui.components.PremiumSectionHeader
import com.baverika.r_journal.ui.theme.AppFontStyle
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.InstrumentSerifFamily
import com.baverika.r_journal.ui.theme.InterFamily
import com.baverika.r_journal.ui.theme.IvoryPalette
import com.baverika.r_journal.ui.theme.LocalDesignTokens
import com.baverika.r_journal.ui.theme.ObsidianPalette
import com.baverika.r_journal.ui.theme.OceanPalette
import com.baverika.r_journal.ui.theme.PlusJakartaSansFamily
import com.baverika.r_journal.ui.theme.SpaceGlassPalette
import com.baverika.r_journal.ui.theme.pressScale
import com.baverika.r_journal.utils.BiometricHelper
import com.baverika.r_journal.utils.MilestoneCalculator
import com.baverika.r_journal.widget.WidgetUpdateUtils
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    passwordRepo: PasswordRepository? = null,
    journalRepo: JournalRepository,
    taskRepo: TaskRepository,
    navController: NavController,
    onThemeChanged: (AppTheme) -> Unit = {},
    onFontStyleChanged: (AppFontStyle) -> Unit = {}
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
    var currentFontStyle by remember { mutableStateOf(settingsRepo.appFontStyle) }
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
            PremiumSectionHeader(title = "Appearance")
            Spacer(modifier = Modifier.height(6.dp))

            PremiumCard {
                Text(
                    text = "Theme Gallery",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Curated surfaces with smooth color transitions",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOption(
                        theme = AppTheme.OBSIDIAN,
                        backgroundColor = ObsidianPalette.Background,
                        surfaceColor = ObsidianPalette.SurfaceContainerHigh,
                        primaryColor = ObsidianPalette.Primary,
                        isSelected = currentTheme == AppTheme.OBSIDIAN,
                        onClick = {
                            currentTheme = AppTheme.OBSIDIAN
                            settingsRepo.appTheme = AppTheme.OBSIDIAN
                            onThemeChanged(AppTheme.OBSIDIAN)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        theme = AppTheme.IVORY,
                        backgroundColor = IvoryPalette.Background,
                        surfaceColor = IvoryPalette.Surface,
                        primaryColor = IvoryPalette.Primary,
                        isSelected = currentTheme == AppTheme.IVORY,
                        onClick = {
                            currentTheme = AppTheme.IVORY
                            settingsRepo.appTheme = AppTheme.IVORY
                            onThemeChanged(AppTheme.IVORY)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ThemeOption(
                        theme = AppTheme.OCEAN,
                        backgroundColor = OceanPalette.Background,
                        surfaceColor = OceanPalette.SurfaceContainerHigh,
                        primaryColor = OceanPalette.Primary,
                        isSelected = currentTheme == AppTheme.OCEAN,
                        onClick = {
                            currentTheme = AppTheme.OCEAN
                            settingsRepo.appTheme = AppTheme.OCEAN
                            onThemeChanged(AppTheme.OCEAN)
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOption(
                        theme = AppTheme.SPACE,
                        backgroundColor = SpaceGlassPalette.Background,
                        surfaceColor = SpaceGlassPalette.SurfaceContainerHigh,
                        primaryColor = SpaceGlassPalette.Primary,
                        isSelected = currentTheme == AppTheme.SPACE,
                        onClick = {
                            currentTheme = AppTheme.SPACE
                            settingsRepo.appTheme = AppTheme.SPACE
                            onThemeChanged(AppTheme.SPACE)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = LocalDesignTokens.current.borderSubtle
                )

                Text(
                    text = "Typography Style",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Choose how headers and journal prose feel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFontStyle.entries.forEach { fontStyle ->
                        FontStyleOption(
                            fontStyle = fontStyle,
                            isSelected = currentFontStyle == fontStyle,
                            onClick = {
                                currentFontStyle = fontStyle
                                settingsRepo.appFontStyle = fontStyle
                                onFontStyleChanged(fontStyle)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
    theme: AppTheme,
    backgroundColor: Color,
    surfaceColor: Color,
    primaryColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightBackground = !theme.isDark
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isLightBackground -> Color(0x261E1B18)
            else -> Color(0x2EFFFFFF)
        },
        animationSpec = tween(220),
        label = "themeBorder"
    )

    Surface(
        modifier = modifier
            .height(92.dp)
            .pressScale(pressedScale = 0.96f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 28.dp, height = 5.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
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
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.45f))
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(surfaceColor)
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                Text(
                    text = theme.displayName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isLightBackground) Color(0xFF1E1B18) else Color(0xFFF3F4F6)
                )
                Text(
                    text = theme.tagline,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isLightBackground) Color(0xFF5A544C) else Color(0xFF9CA3AF)
                )
            }
        }
    }
}

@Composable
private fun FontStyleOption(
    fontStyle: AppFontStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tokens = LocalDesignTokens.current
    val previewFamily = when (fontStyle) {
        AppFontStyle.MODERN -> PlusJakartaSansFamily
        AppFontStyle.EDITORIAL -> InstrumentSerifFamily
        AppFontStyle.CRISP -> InterFamily
    }
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) tokens.highlightFill else tokens.cardElevated,
        animationSpec = tween(220),
        label = "fontBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else tokens.borderSubtle,
        animationSpec = tween(220),
        label = "fontBorder"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.98f)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fontStyle.sampleHeadline,
                    fontFamily = previewFamily,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${fontStyle.displayName} • ${fontStyle.subtitle}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(13.dp)
                    )
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
