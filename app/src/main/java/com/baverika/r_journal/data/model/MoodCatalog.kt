package com.baverika.r_journal.data.model

import androidx.compose.ui.graphics.Color

data class MoodDefinition(
    val id: String,
    val emoji: String,
    val label: String,
    val heatmapName: String,
    val colorDark: Color,
    val colorLight: Color,
    val valenceScore: Int,
    val isCoreInline: Boolean = true
) {
    fun accentColor(isDark: Boolean): Color = if (isDark) colorDark else colorLight
}

data class MoodSelectionState(
    val primaryMood: String? = null,
    val secondaryMoods: List<String> = emptyList()
) {
    val allSelectedIds: List<String>
        get() = listOfNotNull(primaryMood) + secondaryMoods

    val hasSelection: Boolean
        get() = primaryMood != null || secondaryMoods.isNotEmpty()
}

object MoodCatalog {
    const val MOOD_TAG_PREFIX = "#mood-"
    const val MAX_SECONDARY_MOODS = 2

    val all: List<MoodDefinition> = listOf(
        MoodDefinition(
            id = "happy",
            emoji = "😊",
            label = "Happy",
            heatmapName = "Happy",
            colorDark = Color(0xFFFFC107),
            colorLight = Color(0xFFD97706),
            valenceScore = 5,
            isCoreInline = true
        ),
        MoodDefinition(
            id = "calm",
            emoji = "😌",
            label = "Calm",
            heatmapName = "Calm",
            colorDark = Color(0xFF2DD4BF),
            colorLight = Color(0xFF0D9488),
            valenceScore = 4,
            isCoreInline = true
        ),
        MoodDefinition(
            id = "anxious",
            emoji = "😰",
            label = "Anxious",
            heatmapName = "Anxious",
            colorDark = Color(0xFFFB923C),
            colorLight = Color(0xFFEA580C),
            valenceScore = 2,
            isCoreInline = true
        ),
        MoodDefinition(
            id = "sad",
            emoji = "😔",
            label = "Low",
            heatmapName = "Sad",
            colorDark = Color(0xFF60A5FA),
            colorLight = Color(0xFF2563EB),
            valenceScore = 1,
            isCoreInline = true
        ),
        MoodDefinition(
            id = "tired",
            emoji = "😴",
            label = "Tired",
            heatmapName = "Tired",
            colorDark = Color(0xFFA78BFA),
            colorLight = Color(0xFF7C3AED),
            valenceScore = 3,
            isCoreInline = true
        ),
        MoodDefinition(
            id = "excited",
            emoji = "🤩",
            label = "Excited",
            heatmapName = "Excited",
            colorDark = Color(0xFFFB7185),
            colorLight = Color(0xFFE11D48),
            valenceScore = 5,
            isCoreInline = false
        )
    )

    val coreInline: List<MoodDefinition> = all.filter { it.isCoreInline }

    fun findById(id: String?): MoodDefinition? {
        if (id.isNullOrBlank()) return null
        val normalized = id.trim().lowercase()
        return all.firstOrNull { it.id == normalized || it.heatmapName.lowercase() == normalized }
    }

    fun emojiForTagOrId(raw: String): String {
        val id = raw.removePrefix(MOOD_TAG_PREFIX).trim().lowercase()
        return findById(id)?.emoji ?: "😶"
    }

    fun scoreForTagOrId(raw: String): Int {
        val id = raw.removePrefix(MOOD_TAG_PREFIX).trim().lowercase()
        return findById(id)?.valenceScore ?: 0
    }

    fun extractSelection(tags: List<String>): MoodSelectionState {
        val moodIds = tags
            .filter { it.startsWith(MOOD_TAG_PREFIX) }
            .map { it.removePrefix(MOOD_TAG_PREFIX).trim().lowercase() }
            .filter { it.isNotEmpty() }
            .distinct()

        return if (moodIds.isEmpty()) {
            MoodSelectionState()
        } else {
            MoodSelectionState(
                primaryMood = moodIds.first(),
                secondaryMoods = moodIds.drop(1).take(MAX_SECONDARY_MOODS)
            )
        }
    }

    fun applyPrimaryMood(tags: List<String>, moodId: String): List<String> {
        val normalized = moodId.trim().lowercase()
        val nonMoodTags = tags.filterNot { it.startsWith(MOOD_TAG_PREFIX) }
        val current = extractSelection(tags)

        val nextOrderedMoods = if (current.primaryMood == normalized) {
            // Deselect primary: promote first secondary mood if present
            current.secondaryMoods
        } else {
            // Set as primary and ensure it's not duplicated in secondary
            listOf(normalized) + current.secondaryMoods.filterNot { it == normalized }.take(MAX_SECONDARY_MOODS)
        }

        return nonMoodTags + nextOrderedMoods.map { "$MOOD_TAG_PREFIX$it" }
    }

    fun applySecondaryMood(tags: List<String>, moodId: String): List<String> {
        val normalized = moodId.trim().lowercase()
        val nonMoodTags = tags.filterNot { it.startsWith(MOOD_TAG_PREFIX) }
        val current = extractSelection(tags)

        if (current.primaryMood == null) {
            return nonMoodTags + "$MOOD_TAG_PREFIX$normalized"
        }

        if (current.primaryMood == normalized) {
            return tags
        }

        val nextSecondary = if (normalized in current.secondaryMoods) {
            current.secondaryMoods.filterNot { it == normalized }
        } else {
            if (current.secondaryMoods.size >= MAX_SECONDARY_MOODS) {
                return tags
            }
            current.secondaryMoods + normalized
        }

        val ordered = listOf(current.primaryMood) + nextSecondary
        return nonMoodTags + ordered.map { "$MOOD_TAG_PREFIX$it" }
    }

    fun clearMoods(tags: List<String>): List<String> {
        return tags.filterNot { it.startsWith(MOOD_TAG_PREFIX) }
    }

    fun moodSummaryString(tags: List<String>): String? {
        val selected = extractSelection(tags).allSelectedIds
        return if (selected.isNotEmpty()) selected.joinToString(",") else null
    }
}
