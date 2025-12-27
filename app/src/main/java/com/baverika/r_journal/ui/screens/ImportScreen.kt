package com.baverika.r_journal.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.QuickNoteRepository
import com.baverika.r_journal.utils.DbRestoreUtils
import com.baverika.r_journal.utils.ImportUtils
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

@Composable
fun ImportScreen(
    journalRepo: JournalRepository,
    quickNoteRepo: QuickNoteRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isImporting by remember { mutableStateOf(false) }
    var importSuccess by remember { mutableStateOf<Boolean?>(null) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    
    // Auto-backups
    var backups by remember { mutableStateOf<List<File>>(emptyList()) }
    var showRestoreDialog by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(Unit) {
        backups = DbRestoreUtils.getBackups(context)
    }

    // Launcher for picking a ZIP file
    val pickZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            isImporting = true
            ImportUtils.importFromUri(
                context = context,
                uri = pickedUri,
                journalRepo = journalRepo,
                quickNoteRepo = quickNoteRepo,
                coroutineScope = scope
            ) { success, message ->
                isImporting = false
                importSuccess = success
                importMessage = message
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isImporting) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(24.dp))
                Text("Restoring Data...", style = MaterialTheme.typography.titleLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Text(
                        text = "Data Restoration",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Restore from a ZIP file or an automatic backup.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Import ZIP Section
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Import from ZIP",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select a previously exported ZIP file to merge with current data.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { pickZipLauncher.launch(arrayOf("application/zip")) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Select ZIP File")
                            }
                        }
                    }
                }

                // Auto-Backups Section
                item {
                    Text(
                        text = "Automatic Backups",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                if (backups.isEmpty()) {
                    item {
                        Text(
                            text = "No automatic backups found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                } else {
                    items(backups) { file ->
                        BackupItem(file = file, onClick = { showRestoreDialog = file })
                    }
                }
            }
        }

        // Restore Confirmation Dialog
        if (showRestoreDialog != null) {
            AlertDialog(
                onDismissRequest = { showRestoreDialog = null },
                title = { Text("Restore Backup?") },
                text = {
                    Text("This will overwrite your current data with the backup from:\n\n${formatDate(showRestoreDialog!!.lastModified())}\n\nThe app will restart immediately after restoration.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val fileToRestore = showRestoreDialog!!
                            showRestoreDialog = null
                            isImporting = true
                            
                            // Perform restore
                            val success = DbRestoreUtils.restoreBackup(context, fileToRestore)
                            
                            if (success) {
                                // Restart App
                                val packageManager = context.packageManager
                                val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                val componentName = intent?.component
                                val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                                context.startActivity(mainIntent)
                                Runtime.getRuntime().exit(0)
                            } else {
                                isImporting = false
                                importSuccess = false
                                importMessage = "Failed to restore database file."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Restore & Restart")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Success/Error Dialogs (from ZIP import)
        if (importSuccess != null) {
            AlertDialog(
                onDismissRequest = { importSuccess = null },
                title = { Text(if (importSuccess == true) "Success" else "Error") },
                text = { Text(importMessage ?: "") },
                confirmButton = {
                    TextButton(onClick = { importSuccess = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun BackupItem(file: File, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Restore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = formatDate(file.lastModified()),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "${file.length() / 1024} KB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault()).format(Date(timestamp))
}