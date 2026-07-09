package com.baverika.r_journal.ui.viewmodel

import android.app.Application
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.baverika.r_journal.data.local.entity.Tracker
import com.baverika.r_journal.data.local.entity.TrackerHistory
import com.baverika.r_journal.repository.TrackerRepository
import com.baverika.r_journal.widget.TrackerWidgetProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrackerViewModel(
    application: Application,
    private val repository: TrackerRepository
) : AndroidViewModel(application) {

    val trackers: StateFlow<List<Tracker>> = repository.allTrackersFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun updateWidget() {
        val context = getApplication<Application>()
        val intent = Intent(context, TrackerWidgetProvider::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
            ComponentName(context, TrackerWidgetProvider::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

    fun addTracker(
        title: String,
        emoji: String,
        color: Long,
        goal: Int,
        incrementStep: Int,
        resetFrequency: String,
        notes: String? = null
    ) {
        viewModelScope.launch {
            val tracker = Tracker(
                title = title,
                emoji = emoji,
                color = color,
                goal = goal,
                incrementStep = incrementStep,
                resetFrequency = resetFrequency,
                notes = notes
            )
            repository.insertTracker(tracker)
            updateWidget()
        }
    }

    fun updateTracker(tracker: Tracker) {
        viewModelScope.launch {
            repository.updateTracker(tracker)
            updateWidget()
        }
    }

    fun deleteTracker(tracker: Tracker) {
        viewModelScope.launch {
            repository.deleteTracker(tracker)
            updateWidget()
        }
    }

    fun archiveTracker(tracker: Tracker, isArchived: Boolean) {
        viewModelScope.launch {
            repository.updateTracker(tracker.copy(archived = isArchived))
            updateWidget()
        }
    }

    fun incrementTracker(trackerId: String) {
        viewModelScope.launch {
            val tracker = repository.getTrackerById(trackerId) ?: return@launch
            repository.incrementTracker(trackerId, tracker.incrementStep)
            updateWidget()
        }
    }

    fun decrementTracker(trackerId: String) {
        viewModelScope.launch {
            val tracker = repository.getTrackerById(trackerId) ?: return@launch
            repository.decrementTracker(trackerId, tracker.incrementStep)
            updateWidget()
        }
    }

    fun resetTracker(trackerId: String) {
        viewModelScope.launch {
            repository.resetTracker(trackerId)
            updateWidget()
        }
    }

    fun getTrackerByIdFlow(id: String): Flow<Tracker?> = repository.getTrackerByIdFlow(id)

    suspend fun getTrackerById(id: String): Tracker? = repository.getTrackerById(id)

    fun getHistoryForTracker(trackerId: String): Flow<List<TrackerHistory>> = repository.getHistoryForTracker(trackerId)
}
