package com.baverika.r_journal.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.baverika.r_journal.MainActivity
import com.baverika.r_journal.data.local.database.JournalDatabase
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.repository.SettingsRepository
import com.baverika.r_journal.repository.TrackerRepository
import com.baverika.r_journal.ui.theme.RJournalTheme
import com.baverika.r_journal.utils.BiometricHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TrackerWidgetConfigureActivity : FragmentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var settingsRepo: SettingsRepository
    private var isLocked by mutableStateOf(true)
    private var lockErrorMessage by mutableStateOf<String?>(null)

    override fun onStart() {
        super.onStart()
        MainActivity.updateSecureWindowFlag(this, settingsRepo.isBiometricEnabled)
        if (isLocked && settingsRepo.isBiometricEnabled && BiometricHelper.isBiometricAvailable(this)) {
            lockErrorMessage = null
            BiometricHelper.authenticate(
                activity = this,
                onSuccess = {
                    isLocked = false
                    lockErrorMessage = null
                },
                onError = { /* Keep locked until authenticated */ }
            )
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && settingsRepo.isBiometricEnabled) {
            isLocked = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set RESULT_CANCELED. This will cause the widget host to cancel out of the widget placement if the user backs out.
        setResult(RESULT_CANCELED)

        settingsRepo = SettingsRepository(this)
        isLocked = settingsRepo.isBiometricEnabled
        MainActivity.updateSecureWindowFlag(this, settingsRepo.isBiometricEnabled)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            RJournalTheme(theme = settingsRepo.appTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isLocked && settingsRepo.isBiometricEnabled) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "R-Journal Locked",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = lockErrorMessage ?: "Unlock to configure this widget",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (lockErrorMessage != null) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = {
                                    if (BiometricHelper.isBiometricAvailable(this@TrackerWidgetConfigureActivity)) {
                                        lockErrorMessage = null
                                        BiometricHelper.authenticate(
                                            activity = this@TrackerWidgetConfigureActivity,
                                            onSuccess = {
                                                isLocked = false
                                                lockErrorMessage = null
                                            },
                                            onError = {
                                                lockErrorMessage = "Authentication cancelled or failed."
                                            }
                                        )
                                    } else {
                                        lockErrorMessage = "Biometric or device credential unlock is unavailable."
                                    }
                                }
                            ) {
                                Text("Unlock")
                            }
                        }
                    } else {
                        TrackerConfigurationScreen(
                            onTrackerSelected = { tracker ->
                                selectTracker(tracker)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun selectTracker(tracker: Tracker) {
        val context = applicationContext
        saveTrackerPref(context, appWidgetId, tracker.id)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        TrackerSingleWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }

    companion object {
        private const val PREFS_NAME = "com.baverika.r_journal.widget.TrackerSingleWidget"
        private const val PREF_PREFIX_KEY = "single_tracker_id_"

        fun saveTrackerPref(context: Context, appWidgetId: Int, trackerId: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(PREF_PREFIX_KEY + appWidgetId, trackerId)
                .apply()
        }

        fun loadTrackerPref(context: Context, appWidgetId: Int): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(PREF_PREFIX_KEY + appWidgetId, null)
        }

        fun deleteTrackerPref(context: Context, appWidgetId: Int) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(PREF_PREFIX_KEY + appWidgetId)
                .apply()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackerConfigurationScreen(
    onTrackerSelected: (Tracker) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var trackers by remember { mutableStateOf<List<Tracker>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = JournalDatabase.getDatabase(context)
            val repo = TrackerRepository(db.trackerDao())
            trackers = repo.getAllTrackersSync()
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Individual Tracker Widget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select a tracker to display. Tapping anywhere on the widget will update its count.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (trackers.isEmpty()) {
                Text(
                    text = "No trackers available. Open the app to create a tracker first!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(trackers, key = { it.id }) { tracker ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTrackerSelected(tracker) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tracker.emoji,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tracker.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Goal: ${tracker.currentCount} / ${tracker.goal}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
