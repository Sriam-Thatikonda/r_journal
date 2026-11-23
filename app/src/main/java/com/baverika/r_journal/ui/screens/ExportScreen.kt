package com.baverika.r_journal.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.repository.JournalRepository
import com.baverika.r_journal.repository.QuickNoteRepository
import com.baverika.r_journal.repository.PasswordRepository
import com.baverika.r_journal.utils.ExportUtils
import com.baverika.r_journal.utils.PdfExportUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportScreen(
    journalRepo: JournalRepository,
    quickNoteRepo: QuickNoteRepository,
    taskRepo: com.baverika.r_journal.repository.TaskRepository,
    quoteRepo: com.baverika.r_journal.quotes.data.QuoteRepository,
    lifeTrackerRepo: com.baverika.r_journal.repository.LifeTrackerRepository,
    eventRepo: com.baverika.r_journal.repository.EventRepository,
    passwordRepo: PasswordRepository,
    trackerRepo: com.baverika.r_journal.repository.TrackerRepository,
    challengeRepo: com.baverika.r_journal.repository.ChallengeRepository,
    context: Context
) {
    val journals by journalRepo.allEntries.collectAsState(initial = emptyList())
    val notes by quickNoteRepo.allNotes.collectAsState(initial = emptyList())
    val taskCategories by taskRepo.allCategories.collectAsState(initial = emptyList())
    val tasks by taskRepo.allTasks.collectAsState(initial = emptyList())
    val habits by journalRepo.allHabits.collectAsState(initial = emptyList())
    val habitLogs by journalRepo.allHabitLogs.collectAsState(initial = emptyList())
    val quotes by quoteRepo.getAllQuotes().collectAsState(initial = emptyList())
    val lifeTrackers by lifeTrackerRepo.allTrackers.collectAsState(initial = emptyList())
    val lifeTrackerEntries by lifeTrackerRepo.allEntries.collectAsState(initial = emptyList())
    val events by eventRepo.allEvents.collectAsState(initial = emptyList())
    val passwords by passwordRepo.allPasswords.collectAsState(initial = emptyList())
    val trackers by trackerRepo.allTrackersFlow.collectAsState(initial = emptyList())
    val challenges by challengeRepo.getAllChallengesFlow().collectAsState(initial = emptyList())
    val mineEntryState by produceState<com.baverika.r_journal.data.local.entity.JournalEntry?>(initialValue = null) {
        value = journalRepo.getEntryById("mine")
    }
    val mineMessageCount = mineEntryState?.messages?.size ?: 0

    // Selective export checkboxes
    var exportJournals by remember { mutableStateOf(true) }
    var exportMine by remember { mutableStateOf(true) }
    var exportNotes by remember { mutableStateOf(true) }
    var exportTasks by remember { mutableStateOf(true) }
    var exportHabits by remember { mutableStateOf(true) }
    var exportQuotes by remember { mutableStateOf(true) }
    var exportLifeTrackers by remember { mutableStateOf(true) }
    var exportTrackers by remember { mutableStateOf(true) }
    var exportChallenges by remember { mutableStateOf(true) }
    var exportEvents by remember { mutableStateOf(true) }
    var exportPasswords by remember { mutableStateOf(true) }

    val allSelected = exportJournals && exportMine && exportNotes && exportTasks && exportHabits &&
            exportQuotes && exportLifeTrackers && exportTrackers && exportChallenges &&
            exportEvents && exportPasswords

    val hasAnySelected = exportJournals || exportMine || exportNotes || exportTasks || exportHabits ||
            exportQuotes || exportLifeTrackers || exportTrackers || exportChallenges ||
            exportEvents || exportPasswords

    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var exportSuccess by remember { mutableStateOf<Boolean?>(null) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                isExporting -> {
                    // Exporting state
                    CircularProgressIndicator(modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Exporting Selected Data...",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This may take a moment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                exportSuccess == true -> {
                    // Success state
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Export Successful!",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportMessage ?: "Your data has been exported successfully.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { exportSuccess = null }) {
                        Text("Done")
                    }
                }

                exportSuccess == false -> {
                    // Error state
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Export Failed",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exportMessage ?: "An error occurred during export.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { exportSuccess = null }) {
                        Text("Try Again")
                    }
                }

                else -> {
                    // Initial state
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Selective Data Export",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choose which modules to include in your backup ZIP or export a PDF book",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    // Module Selection Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SELECT MODULES",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        val next = !allSelected
                                        exportJournals = next
                                        exportMine = next
                                        exportNotes = next
                                        exportTasks = next
                                        exportHabits = next
                                        exportQuotes = next
                                        exportLifeTrackers = next
                                        exportTrackers = next
                                        exportChallenges = next
                                        exportEvents = next
                                        exportPasswords = next
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (allSelected) "Deselect All" else "Select All",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val exportItems = listOf(
                                ExportItem("📖", "Journals", journals.size, exportJournals) { exportJournals = !exportJournals },
                                ExportItem("💭", "Mine", mineMessageCount, exportMine) { exportMine = !exportMine },
                                ExportItem("📝", "Notes", notes.size, exportNotes) { exportNotes = !exportNotes },
                                ExportItem("☑️", "Tasks", tasks.size, exportTasks) { exportTasks = !exportTasks },
                                ExportItem("🔥", "Habits", habits.size, exportHabits) { exportHabits = !exportHabits },
                                ExportItem("💬", "Quotes", quotes.size, exportQuotes) { exportQuotes = !exportQuotes },
                                ExportItem("🏆", "Milestones", lifeTrackers.size, exportLifeTrackers) { exportLifeTrackers = !exportLifeTrackers },
                                ExportItem("📊", "Trackers", trackers.size, exportTrackers) { exportTrackers = !exportTrackers },
                                ExportItem("🎯", "Challenges", challenges.size, exportChallenges) { exportChallenges = !exportChallenges },
                                ExportItem("🎉", "Events", events.size, exportEvents) { exportEvents = !exportEvents },
                                ExportItem("🔒", "Vault", passwords.size, exportPasswords) { exportPasswords = !exportPasswords }
                            )

                            val chunked = exportItems.chunked(2)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                chunked.forEach { rowItems ->
                                    if (rowItems.size == 2) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ExportModuleCard(
                                                item = rowItems[0],
                                                modifier = Modifier.weight(1f)
                                            )
                                            ExportModuleCard(
                                                item = rowItems[1],
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    } else {
                                        // One left out -> full scale!
                                        ExportModuleCard(
                                            item = rowItems[0],
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                val trackerHistory = if (exportTrackers) trackerRepo.getAllHistorySync() else emptyList()
                                val challengeEntities = if (exportChallenges) challengeRepo.getAllChallengesSync() else emptyList()
                                val mineEntry = if (exportMine) journalRepo.getEntryById("mine") else null
                                val (success, message) = ExportUtils.exportAll(
                                    context,
                                    if (exportJournals) journals else emptyList(),
                                    if (exportNotes) notes else emptyList(),
                                    if (exportTasks) taskCategories else emptyList(),
                                    if (exportTasks) tasks else emptyList(),
                                    if (exportHabits) habits else emptyList(),
                                    if (exportHabits) habitLogs else emptyList(),
                                    if (exportQuotes) quotes else emptyList(),
                                    if (exportLifeTrackers) lifeTrackers else emptyList(),
                                    if (exportLifeTrackers) lifeTrackerEntries else emptyList(),
                                    if (exportEvents) events else emptyList(),
                                    if (exportPasswords) passwords else emptyList(),
                                    if (exportTrackers) trackers else emptyList(),
                                    trackerHistory,
                                    challengeEntities,
                                    mineEntry
                                )
                                isExporting = false
                                exportSuccess = success
                                exportMessage = message
                                if (success) {
                                    com.baverika.r_journal.repository.SettingsRepository(context).lastBackupTimestamp = System.currentTimeMillis()
                                }
                            }
                        },
                        enabled = hasAnySelected,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (hasAnySelected) "Export Selected (ZIP)" else "Select at least 1 module")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // PDF Export Button
                    OutlinedButton(
                        onClick = {
                            isExporting = true
                            scope.launch {
                                val mineEntry = if (exportMine) journalRepo.getEntryById("mine") else null
                                val (success, message) = PdfExportUtils.exportToPdf(
                                    context,
                                    if (exportJournals) journals else emptyList(),
                                    if (exportNotes) notes else emptyList(),
                                    mineEntry
                                )
                                isExporting = false
                                exportSuccess = success
                                exportMessage = message
                                if (success) {
                                    com.baverika.r_journal.repository.SettingsRepository(context).lastBackupTimestamp = System.currentTimeMillis()
                                }
                            }
                        },
                        enabled = exportJournals || exportNotes || exportMine,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Journals & Notes as PDF Book")
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter)
        )
    }
}

private data class ExportItem(
    val emoji: String,
    val title: String,
    val count: Int,
    val isSelected: Boolean,
    val onToggle: () -> Unit
)

@Composable
private fun ExportModuleCard(
    item: ExportItem,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = item.onToggle,
        shape = RoundedCornerShape(12.dp),
        color = if (item.isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (item.isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Text(text = item.emoji, fontSize = 16.sp)
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (item.isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (item.isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (item.isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = "${item.count}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { item.onToggle() },
                    modifier = Modifier.size(20.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            }
        }
    }
}
