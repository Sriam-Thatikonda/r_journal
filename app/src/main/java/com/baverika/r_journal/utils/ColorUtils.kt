package com.baverika.r_journal.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Utility functions for color manipulation and readability
 */
object ColorUtils {
    
    /**
     * Determines if the given color is "light" based on its luminance
     * Returns true if the color is light (needs dark text)
     * Returns false if the color is dark (needs light text)
     */
    fun isColorLight(color: Color): Boolean {
        return color.luminance() > 0.5f
    }
    
    /**
     * Returns the appropriate text color (black or white) based on background color
     * for optimal readability
     */
    fun getContrastingTextColor(backgroundColor: Color): Color {
        return if (isColorLight(backgroundColor)) {
            Color(0xFF1F1F1F) // Dark text for light backgrounds
        } else {
            Color(0xFFFAFAFA) // Light text for dark backgrounds
        }
    }
    
    /**
     * Returns a slightly darker version of the color for secondary text
     */
    fun getSecondaryTextColor(backgroundColor: Color): Color {
        return if (isColorLight(backgroundColor)) {
            Color(0xFF5F5F5F) // Medium gray for light backgrounds
        } else {
            Color(0xFFB0B0B0) // Light gray for dark backgrounds
        }
    }
}
