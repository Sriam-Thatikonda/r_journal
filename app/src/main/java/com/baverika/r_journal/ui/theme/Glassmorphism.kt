package com.baverika.r_journal.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Design tokens and utility helpers for the Space (Rosewood) Pure Black Glassmorphism theme.
 */
object SpaceGlassTokens {
    // Pure black & near-black
    val PureBlack = Color(0xFF000000)
    val NearBlack = Color(0xFF050505)

    // Glass surfaces
    val CardGlass = Color(0xB3101216)          // ~70% dark charcoal glass card
    val CardGlassElevated = Color(0xCC14161C)  // ~80% elevated glass card
    val SurfaceGlass = Color(0xCC090A0D)       // ~80% dark charcoal glass surface
    val InputGlass = Color(0x6614161B)         // ~40% input field glass
    val BarGlass = Color(0xE6050505)           // ~90% bottom / top bar glass
    val SheetGlass = Color(0xF208090C)         // ~95% dialog / bottom sheet glass
    val ButtonGlass = Color(0x26FFFFFF)        // ~15% frosted glass button
    val HighlightGlass = Color(0x26FFFFFF)     // ~15% restrained selection highlight

    // Glass borders
    val BorderSubtle = Color(0x1FFFFFFF)       // ~12% white
    val BorderDefault = Color(0x2EFFFFFF)      // ~18% white 1px border
    val BorderProminent = Color(0x4DFFFFFF)    // ~30% white (focused / active)

    // BorderStrokes
    val StrokeSubtle = BorderStroke(1.dp, BorderSubtle)
    val StrokeDefault = BorderStroke(1.dp, BorderDefault)
    val StrokeProminent = BorderStroke(1.dp, BorderProminent)

    // Accents & Typography
    val SilverAccent = Color(0xFFEDEDED)
    val TextPrimary = Color(0xFFF3F4F6)
    val TextSecondary = Color(0xFF9CA3AF)
    val TextMuted = Color(0xFF6B7280)
}

/**
 * Extension property to check if this theme is Space (Rosewood).
 */
val AppTheme.isSpace: Boolean
    get() = this == AppTheme.ROSEWOOD

/**
 * Composable helper to check if active theme is Space (Rosewood).
 */
@Composable
fun isSpaceTheme(): Boolean = LocalAppTheme.current == AppTheme.ROSEWOOD

