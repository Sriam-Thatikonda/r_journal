package com.baverika.r_journal.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.util.Calendar
import android.app.DatePickerDialog
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
import com.baverika.r_journal.utils.PasswordExportUtils
import com.baverika.r_journal.BuildConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepo: SettingsRepository,
    passwordRepo: PasswordRepository,
    navController: NavController,
    onThemeChanged: (AppTheme) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isBiometricEnabled by remember { mutableStateOf(settingsRepo.isBiometricEnabled) }
    var currentTheme by remember { mutableStateOf(settingsRepo.appTheme) }
    var birthDay by remember { mutableIntStateOf(settingsRepo.birthDay) }
    var birthMonth by remember { mutableIntStateOf(settingsRepo.birthMonth) }
    var birthYear by remember { mutableIntStateOf(settingsRepo.birthYear) }
    
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // Date Picker Logic
    val calendar = Calendar.getInstance()
    calendar.set(birthYear, birthMonth - 1, birthDay)
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            birthYear = year
            birthMonth = month + 1
            birthDay = dayOfMonth
            
            settingsRepo.birthYear = year
            settingsRepo.birthMonth = month + 1
            settingsRepo.birthDay = dayOfMonth
        },
        birthYear,
        birthMonth - 1,
        birthDay
    )

    val passwords by passwordRepo.allPasswords.collectAsState(initial = emptyList())

    val passwordImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            isImporting = true
            scope.launch {
                val (_, message) = PasswordExportUtils.importPasswords(context, it, passwordRepo)
                isImporting = false
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .fillMaxSize()
                .padding(padding)
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
                    name = "Rose",
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
                    onCheckedChange = {
                        isBiometricEnabled = it
                        settingsRepo.isBiometricEnabled = it
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Data Management Section ---
            Text(
                text = "Data Management",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Upload,
                title = "Export Data",
                subtitle = "Backup journals and notes",
                onClick = { navController.navigate("export") }
            )

            SettingsItem(
                icon = Icons.Default.Download,
                title = "Import Data",
                subtitle = "Restore from backup",
                onClick = { navController.navigate("import") }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // --- Password Management Section ---
            Text(
                text = "Password Manager",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            SettingsItem(
                icon = Icons.Default.Upload,
                title = "Export Passwords",
                subtitle = if (passwords.isEmpty()) "No passwords saved" else "${passwords.size} passwords",
                enabled = passwords.isNotEmpty() && !isExporting,
                onClick = {
                    isExporting = true
                    scope.launch {
                        val (_, message) = PasswordExportUtils.exportPasswords(context, passwords)
                        isExporting = false
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            )

            SettingsItem(
                icon = Icons.Default.Download,
                title = "Import Passwords",
                subtitle = "Restore from backup",
                enabled = !isImporting,
                onClick = {
                    passwordImportLauncher.launch(arrayOf("application/json", "*/*"))
                }
            )

            if (isExporting || isImporting) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isExporting) "Exporting..." else "Importing...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
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
                onClick = { datePickerDialog.show() }
            )

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
            .height(64.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(10.dp)
                ) else Modifier
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp)
        ) {
            // Primary color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .align(Alignment.TopEnd)
            )

            // Theme name
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isLightBackground)
                    Color(0xFF2C2520)          // Warm dark brown for light backgrounds
                else
                    Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.BottomStart)
            )

            // Check mark if selected
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = primaryColor,
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.TopStart)
                )
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
