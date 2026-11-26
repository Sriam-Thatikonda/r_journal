package com.baverika.r_journal

import com.baverika.r_journal.data.model.MoodCatalog
import com.baverika.r_journal.data.model.MoodSelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoodSelectionTest {

    @Test
    fun moodCatalog_hasAllSixUniqueMoods() {
        assertEquals(6, MoodCatalog.all.size)
        assertEquals(5, MoodCatalog.coreInline.size)
        assertEquals(6, MoodCatalog.all.map { it.id }.toSet().size)
        assertTrue(MoodCatalog.all.all { it.valenceScore in 1..5 })
        assertEquals("🤩", MoodCatalog.emojiForTagOrId("#mood-excited"))
        assertEquals(5, MoodCatalog.scoreForTagOrId("#mood-excited"))
    }

    @Test
    fun applyPrimaryMood_setsPrimaryPromotesSecondaryAndPreservesNonMoodTags() {
        val baseTags = listOf("#gratitude", "#mood-calm", "#mood-tired")

        // Setting a new primary replaces current primary and keeps secondary moods
        val updated = MoodCatalog.applyPrimaryMood(baseTags, "happy")
        assertEquals(listOf("#gratitude", "#mood-happy", "#mood-tired"), updated)
        assertEquals(
            MoodSelectionState(primaryMood = "happy", secondaryMoods = listOf("tired")),
            MoodCatalog.extractSelection(updated)
        )
        assertEquals("happy,tired", MoodCatalog.moodSummaryString(updated))

        // Tapping current primary promotes first secondary mood to primary
        val promoted = MoodCatalog.applyPrimaryMood(updated, "happy")
        assertEquals(MoodSelectionState(primaryMood = "tired", secondaryMoods = emptyList()), MoodCatalog.extractSelection(promoted))

        // Tapping last remaining primary clears mood while preserving non-mood tags
        val cleared = MoodCatalog.applyPrimaryMood(promoted, "tired")
        assertEquals(listOf("#gratitude"), cleared)
        assertNull(MoodCatalog.moodSummaryString(cleared))
    }

    @Test
    fun applySecondaryMood_enforcesTwoSecondaryLimitAndPromotesWhenNoPrimary() {
        // When no primary exists, tapping secondary promotes it to primary
        val first = MoodCatalog.applySecondaryMood(emptyList(), "calm")
        assertEquals(MoodSelectionState(primaryMood = "calm", secondaryMoods = emptyList()), MoodCatalog.extractSelection(first))

        // Add up to 2 secondary moods
        val second = MoodCatalog.applySecondaryMood(first, "tired")
        val third = MoodCatalog.applySecondaryMood(second, "excited")
        assertEquals(
            MoodSelectionState(primaryMood = "calm", secondaryMoods = listOf("tired", "excited")),
            MoodCatalog.extractSelection(third)
        )

        // 3rd secondary mood is ignored when cap (2) is reached
        val capped = MoodCatalog.applySecondaryMood(third, "happy")
        assertEquals(third, capped)

        // Tapping an active secondary mood toggles it off
        val toggledOff = MoodCatalog.applySecondaryMood(third, "tired")
        assertEquals(
            MoodSelectionState(primaryMood = "calm", secondaryMoods = listOf("excited")),
            MoodCatalog.extractSelection(toggledOff)
        )
    }
}
