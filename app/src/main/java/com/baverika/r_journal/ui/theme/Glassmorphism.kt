package com.baverika.r_journal.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified design & glassmorphism tokens shared across all screens and themes.
 */
@Immutable
data class RJournalTokens(
    val isGlassTheme: Boolean,
    val cardRadius: Dp = 20.dp,
    val heroRadius: Dp = 26.dp,
    val sheetRadius: Dp = 28.dp,
    val inputRadius: Dp = 22.dp,
    val pillRadius: Dp = 999.dp,
    val cardContainer: Color,
    val cardElevated: Color,
    val surfaceGlass: Color,
    val inputContainer: Color,
    val barGlass: Color,
    val sheetContainer: Color,
    val highlightFill: Color,
    val borderSubtle: Color,
    val borderDefault: Color,
    val borderProminent: Color,
    val ambientGlowTop: Color,
    val ambientGlowBottom: Color
) {
    val strokeSubtle: BorderStroke get() = BorderStroke(0.75.dp, borderSubtle)
    val strokeDefault: BorderStroke get() = BorderStroke(1.dp, borderDefault)
    val strokeProminent: BorderStroke get() = BorderStroke(1.25.dp, borderProminent)
}

val LocalDesignTokens = staticCompositionLocalOf {
    tokensForTheme(AppTheme.OBSIDIAN)
}

fun tokensForTheme(theme: AppTheme): RJournalTokens {
    return when (theme) {
        AppTheme.OBSIDIAN -> RJournalTokens(
            isGlassTheme = false,
            cardContainer = Color(0xFF0E1015),
            cardElevated = Color(0xFF151820),
            surfaceGlass = Color(0xE6090A0D),
            inputContainer = Color(0xFF12141B),
            barGlass = Color(0xEB050608),
            sheetContainer = Color(0xFF0E1015),
            highlightFill = Color(0x1FFFFFFF),
            borderSubtle = Color(0x16FFFFFF),
            borderDefault = Color(0x24FFFFFF),
            borderProminent = Color(0x40FFFFFF),
            ambientGlowTop = Color(0x0DFFFFFF),
            ambientGlowBottom = Color.Transparent
        )

        AppTheme.IVORY -> RJournalTokens(
            isGlassTheme = false,
            cardContainer = Color(0xFFFCFBF7),
            cardElevated = Color(0xFFFFFFFF),
            surfaceGlass = Color(0xF0F5F2EB),
            inputContainer = Color(0xFFF0ECE3),
            barGlass = Color(0xEBFCFBF7),
            sheetContainer = Color(0xFFFCFBF7),
            highlightFill = Color(0x1A2B5B4A),
            borderSubtle = Color(0x141E1B18),
            borderDefault = Color(0x221E1B18),
            borderProminent = Color(0x4D2B5B4A),
            ambientGlowTop = Color(0x142B5B4A),
            ambientGlowBottom = Color(0x0D9A6859)
        )

        AppTheme.OCEAN -> RJournalTokens(
            isGlassTheme = true,
            cardContainer = Color(0xCC0E2030),
            cardElevated = Color(0xE0142A3F),
            surfaceGlass = Color(0xD90A1724),
            inputContainer = Color(0x99132638),
            barGlass = Color(0xE607131E),
            sheetContainer = Color(0xF50B1926),
            highlightFill = Color(0x2467D4F5),
            borderSubtle = Color(0x1F67D4F5),
            borderDefault = Color(0x3367D4F5),
            borderProminent = Color(0x6667D4F5),
            ambientGlowTop = Color(0x2267D4F5),
            ambientGlowBottom = Color(0x14A5B4FC)
        )

        AppTheme.SPACE -> RJournalTokens(
            isGlassTheme = true,
            cardContainer = Color(0xB3101218),
            cardElevated = Color(0xCC161922),
            surfaceGlass = Color(0xCC08090D),
            inputContainer = Color(0x7314171F),
            barGlass = Color(0xEB050507),
            sheetContainer = Color(0xF50A0B0F),
            highlightFill = Color(0x26FFFFFF),
            borderSubtle = Color(0x1EFFFFFF),
            borderDefault = Color(0x30FFFFFF),
            borderProminent = Color(0x54FFFFFF),
            ambientGlowTop = Color(0x18FFFFFF),
            ambientGlowBottom = Color(0x0C9CA3AF)
        )
    }
}

/**
 * Legacy helper kept for gradual screen migration; maps to Space Glass theme.
 */
val AppTheme.isSpace: Boolean
    get() = this == AppTheme.SPACE

@Composable
fun isSpaceTheme(): Boolean = LocalAppTheme.current == AppTheme.SPACE

@Composable
fun isGlassActive(): Boolean = LocalDesignTokens.current.isGlassTheme

/**
 * Applies a theme-aware premium surface with optional subtle top sheen and hairline border.
 */
fun Modifier.premiumCardSurface(
    shape: Shape,
    elevated: Boolean = false
): Modifier = composed {
    val tokens = LocalDesignTokens.current
    val fill = if (elevated) tokens.cardElevated else tokens.cardContainer
    val borderColor = if (elevated) tokens.borderDefault else tokens.borderSubtle
    this
        .clip(shape)
        .background(fill, shape)
        .then(
            if (tokens.isGlassTheme) {
                Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.06f),
                            Color.Transparent
                        )
                    ),
                    shape
                )
            } else {
                Modifier
            }
        )
        .border(0.9.dp, borderColor, shape)
}
