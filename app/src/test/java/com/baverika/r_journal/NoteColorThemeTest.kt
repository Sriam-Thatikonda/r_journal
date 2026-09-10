package com.baverika.r_journal

import androidx.compose.ui.graphics.Color
import com.baverika.r_journal.data.model.NoteColor
import com.baverika.r_journal.ui.theme.AppTheme
import com.baverika.r_journal.utils.ColorUtils
import org.junit.Assert.*
import org.junit.Test

class NoteColorThemeTest {

    @Test
    fun appTheme_isDark_returnsCorrectMode() {
        assertTrue("MIDNIGHT must be dark", AppTheme.MIDNIGHT.isDark)
        assertTrue("OCEAN must be dark", AppTheme.OCEAN.isDark)
        assertTrue("ROSEWOOD must be dark", AppTheme.ROSEWOOD.isDark)
        assertTrue("BLUE_SKY must be dark", AppTheme.BLUE_SKY.isDark)

        assertFalse("LIGHT must be light", AppTheme.LIGHT.isDark)
        assertFalse("CLOUD_DANCER must be light", AppTheme.CLOUD_DANCER.isDark)
    }

    @Test
    fun noteColor_fromLong_handlesSignedAndUnsignedValues() {
        // Unsigned 32-bit hex literal for white
        assertEquals(NoteColor.DEFAULT, NoteColor.fromLong(0xFFFFFFFFL))
        // Signed 32-bit integer (-1) converted to Long
        assertEquals(NoteColor.DEFAULT, NoteColor.fromLong(-1L))
        // Default black (0xFF000000)
        assertEquals(NoteColor.DEFAULT, NoteColor.fromLong(0xFF000000L))
        assertEquals(NoteColor.DEFAULT, NoteColor.fromLong(0L))

        // Blue dot color
        assertEquals(NoteColor.BLUE, NoteColor.fromLong(NoteColor.BLUE.dotColor))
        // Blue dot color as signed 32-bit Int converted to Long
        val signedBlue = (NoteColor.BLUE.dotColor.toInt()).toLong()
        assertEquals(NoteColor.BLUE, NoteColor.fromLong(signedBlue))

        // Amber
        assertEquals(NoteColor.AMBER, NoteColor.fromLong(NoteColor.AMBER.dotColor))
        // Coral
        assertEquals(NoteColor.CORAL, NoteColor.fromLong(NoteColor.CORAL.dotColor))
        // Green
        assertEquals(NoteColor.GREEN, NoteColor.fromLong(NoteColor.GREEN.dotColor))
        // Purple
        assertEquals(NoteColor.PURPLE, NoteColor.fromLong(NoteColor.PURPLE.dotColor))
        // Teal
        assertEquals(NoteColor.TEAL, NoteColor.fromLong(NoteColor.TEAL.dotColor))

        // Legacy Keep colors
        assertEquals(NoteColor.BLUE, NoteColor.fromLong(0xFFAECBFAL))
        assertEquals(NoteColor.GREEN, NoteColor.fromLong(0xFFCCFF90L))
    }

    @Test
    fun noteColor_containerColorInDarkMode_isNotWhite() {
        for (noteColor in NoteColor.entries) {
            val darkBg = noteColor.containerColor(isDark = true)
            // Ensure none of the dark containers are pure white or light
            assertFalse(
                "${noteColor.displayName} in dark mode should not be light",
                ColorUtils.isColorLight(darkBg)
            )
            // Contrasting text for dark background MUST be light/white text
            val textColor = ColorUtils.getContrastingTextColor(darkBg)
            assertEquals(
                "Text on ${noteColor.displayName} in dark mode must be crisp light text",
                Color(0xFFFAFAFA),
                textColor
            )
        }
    }

    @Test
    fun noteColor_containerColorInLightMode_isLightWithDarkText() {
        for (noteColor in NoteColor.entries) {
            val lightBg = noteColor.containerColor(isDark = false)
            assertTrue(
                "${noteColor.displayName} in light mode should be light",
                ColorUtils.isColorLight(lightBg)
            )
            val textColor = ColorUtils.getContrastingTextColor(lightBg)
            assertEquals(
                "Text on ${noteColor.displayName} in light mode must be dark text",
                Color(0xFF1F1F1F),
                textColor
            )
        }
    }
}
