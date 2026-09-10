package com.baverika.r_journal.data.model

import androidx.compose.ui.graphics.Color

enum class NoteColor(
    val displayName: String,
    val hex: String,
    val lightContainer: Long,
    val darkContainer: Long,
    val lightBorder: Long,
    val darkBorder: Long,
    val dotColor: Long
) {
    DEFAULT(
        displayName = "Default",
        hex = "#71717A",
        lightContainer = 0xFFFFFFFF,
        darkContainer = 0xFF1C1C20,
        lightBorder = 0xFFE4E4E7,
        darkBorder = 0xFF2E2E34,
        dotColor = 0xFF71717A
    ),
    BLUE(
        displayName = "Ocean Blue",
        hex = "#3B82F6",
        lightContainer = 0xFFF0F6FF,
        darkContainer = 0xFF172033,
        lightBorder = 0xFFBFDBFE,
        darkBorder = 0xFF1E3A8A,
        dotColor = 0xFF3B82F6
    ),
    GREEN(
        displayName = "Sage Green",
        hex = "#10B981",
        lightContainer = 0xFFF0FDF4,
        darkContainer = 0xFF14291E,
        lightBorder = 0xFFBBF7D0,
        darkBorder = 0xFF065F46,
        dotColor = 0xFF10B981
    ),
    AMBER(
        displayName = "Sunset Amber",
        hex = "#F59E0B",
        lightContainer = 0xFFFFFBEB,
        darkContainer = 0xFF2D2312,
        lightBorder = 0xFFFDE68A,
        darkBorder = 0xFF78350F,
        dotColor = 0xFFF59E0B
    ),
    CORAL(
        displayName = "Coral Red",
        hex = "#EF4444",
        lightContainer = 0xFFFEF2F2,
        darkContainer = 0xFF2E1517,
        lightBorder = 0xFFFECACA,
        darkBorder = 0xFF7F1D1D,
        dotColor = 0xFFEF4444
    ),
    PURPLE(
        displayName = "Lavender Purple",
        hex = "#8B5CF6",
        lightContainer = 0xFFFAF5FF,
        darkContainer = 0xFF231738,
        lightBorder = 0xFFDDD6FE,
        darkBorder = 0xFF4C1D95,
        dotColor = 0xFF8B5CF6
    ),
    TEAL(
        displayName = "Nordic Teal",
        hex = "#14B8A6",
        lightContainer = 0xFFF0FDFA,
        darkContainer = 0xFF132828,
        lightBorder = 0xFF99F6E4,
        darkBorder = 0xFF115E59,
        dotColor = 0xFF14B8A6
    );

    fun containerColor(isDark: Boolean): Color =
        Color(if (isDark) darkContainer else lightContainer)

    fun borderColor(isDark: Boolean): Color =
        Color(if (isDark) darkBorder else lightBorder)

    fun dot(): Color = Color(dotColor)

    companion object {
        fun fromLong(value: Long): NoteColor {
            val normalized = value and 0xFFFFFFFFL
            return entries.firstOrNull { 
                (it.dotColor and 0xFFFFFFFFL) == normalized || it.ordinal.toLong() == value 
            } ?: when (normalized) {
                0x00000000L, 0x000000FFL, 0xFF000000L, 0xFF1F1F1FL, 0xFFFFFFFFL -> DEFAULT
                0xFFF28B82L -> CORAL
                0xFFFBBC04L -> AMBER
                0xFFFFF475L -> AMBER
                0xFFCCFF90L -> GREEN
                0xFFA7FFEBL -> TEAL
                0xFFAECBFAL -> BLUE
                0xFFD7AEFBL -> PURPLE
                0xFFFDCFE8L -> CORAL
                0xFFE6C9A8L -> AMBER
                0xFFE8EAEDL -> DEFAULT
                else -> entries.firstOrNull { (it.dotColor and 0xFFFFFFFFL) == normalized } ?: DEFAULT
            }
        }
    }
}
