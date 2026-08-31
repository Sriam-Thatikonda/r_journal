package com.baverika.r_journal.data.model

data class MilestoneStats(
    val totalWordsWritten: Int = 0,
    val mostActiveDay: String = "No entries yet",
    val totalTasksCompleted: Int = 0,
    val topMood: String = "None yet",
    val totalDaysJournaled: Int = 0,
    val totalDaysInPeriod: Int = 365,
    val totalImagesAttached: Int = 0,
    val totalAudioNotesRecorded: Int = 0
)
