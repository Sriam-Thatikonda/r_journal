package com.baverika.r_journal

import com.baverika.r_journal.ui.theme.AppFontStyle
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.ui.theme.RouteMotionType
import com.baverika.r_journal.ui.theme.RouteTransitions
import org.junit.Assert.assertEquals
import org.junit.Test

class AppThemeMigrationTest {

    @Test
    fun fromPersistedName_mapsLegacyAndCurrentThemesAccurately() {
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromPersistedName("MIDNIGHT"))
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromPersistedName("OBSIDIAN"))

        assertEquals(AppTheme.IVORY, AppTheme.fromPersistedName("LIGHT"))
        assertEquals(AppTheme.IVORY, AppTheme.fromPersistedName("CLOUD_DANCER"))
        assertEquals(AppTheme.IVORY, AppTheme.fromPersistedName("IVORY"))

        assertEquals(AppTheme.OCEAN, AppTheme.fromPersistedName("OCEAN"))
        assertEquals(AppTheme.OCEAN, AppTheme.fromPersistedName("BLUE_SKY"))

        assertEquals(AppTheme.SPACE, AppTheme.fromPersistedName("ROSEWOOD"))
        assertEquals(AppTheme.SPACE, AppTheme.fromPersistedName("SPACE"))

        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromPersistedName(null))
        assertEquals(AppTheme.OBSIDIAN, AppTheme.fromPersistedName("UNKNOWN_THEME"))
    }

    @Test
    fun appFontStyle_fromPersistedName_resolvesAndFallsBackSafely() {
        assertEquals(AppFontStyle.MODERN, AppFontStyle.fromPersistedName("MODERN"))
        assertEquals(AppFontStyle.EDITORIAL, AppFontStyle.fromPersistedName("editorial"))
        assertEquals(AppFontStyle.CRISP, AppFontStyle.fromPersistedName("CRISP"))
        assertEquals(AppFontStyle.MODERN, AppFontStyle.fromPersistedName(null))
        assertEquals(AppFontStyle.MODERN, AppFontStyle.fromPersistedName("INVALID"))
    }

    @Test
    fun routeTransitions_classifyRoutesIntoExpectedMotionTypes() {
        assertEquals(RouteMotionType.DOCK_TAB, RouteTransitions.classify("archive"))
        assertEquals(RouteMotionType.DOCK_TAB, RouteTransitions.classify("quick_notes"))
        assertEquals(RouteMotionType.DOCK_TAB, RouteTransitions.classify("tasks"))

        assertEquals(RouteMotionType.DRAWER_HUB, RouteTransitions.classify("dashboard"))
        assertEquals(RouteMotionType.DRAWER_HUB, RouteTransitions.classify("settings"))
        assertEquals(RouteMotionType.DRAWER_HUB, RouteTransitions.classify("habits"))

        assertEquals(RouteMotionType.MODAL_SHEET, RouteTransitions.classify("new_quick_note"))
        assertEquals(RouteMotionType.MODAL_SHEET, RouteTransitions.classify("edit_quick_note/{noteId}"))
        assertEquals(RouteMotionType.MODAL_SHEET, RouteTransitions.classify("add_habit?habitId={habitId}"))

        assertEquals(RouteMotionType.MEDIA_VIEWER, RouteTransitions.classify("image_viewer/{encodedPath}"))
        assertEquals(RouteMotionType.DETAIL_PUSH, RouteTransitions.classify("chat_input/{entryId}"))
        assertEquals(RouteMotionType.DETAIL_PUSH, RouteTransitions.classify("habit_detail/{habitId}/{month}"))
    }
}
