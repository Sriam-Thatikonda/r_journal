package com.baverika.r_journal.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Composition local for current theme
val LocalAppTheme = staticCompositionLocalOf { AppTheme.MIDNIGHT }

/**
 * Color scheme for Midnight theme (Dark)
 */
private val MidnightColorScheme = darkColorScheme(
    primary = MidnightColors.Primary,
    onPrimary = MidnightColors.OnPrimary,
    primaryContainer = MidnightColors.PrimaryContainer,
    onPrimaryContainer = MidnightColors.OnPrimaryContainer,
    secondary = MidnightColors.Secondary,
    onSecondary = MidnightColors.OnSecondary,
    secondaryContainer = MidnightColors.SecondaryContainer,
    onSecondaryContainer = MidnightColors.OnSecondaryContainer,
    tertiary = MidnightColors.Tertiary,
    onTertiary = MidnightColors.OnTertiary,
    tertiaryContainer = MidnightColors.TertiaryContainer,
    onTertiaryContainer = MidnightColors.OnTertiaryContainer,
    background = MidnightColors.Background,
    onBackground = MidnightColors.OnBackground,
    surface = MidnightColors.Surface,
    onSurface = MidnightColors.OnSurface,
    surfaceVariant = MidnightColors.SurfaceVariant,
    onSurfaceVariant = MidnightColors.OnSurfaceVariant,
    error = MidnightColors.Error,
    onError = MidnightColors.OnError,
    outline = MidnightColors.Outline
)

/**
 * Color scheme for Light theme
 */
private val LightColorScheme = lightColorScheme(
    primary = LightColors.Primary,
    onPrimary = LightColors.OnPrimary,
    primaryContainer = LightColors.PrimaryContainer,
    onPrimaryContainer = LightColors.OnPrimaryContainer,
    secondary = LightColors.Secondary,
    onSecondary = LightColors.OnSecondary,
    secondaryContainer = LightColors.SecondaryContainer,
    onSecondaryContainer = LightColors.OnSecondaryContainer,
    tertiary = LightColors.Tertiary,
    onTertiary = LightColors.OnTertiary,
    tertiaryContainer = LightColors.TertiaryContainer,
    onTertiaryContainer = LightColors.OnTertiaryContainer,
    background = LightColors.Background,
    onBackground = LightColors.OnBackground,
    surface = LightColors.Surface,
    onSurface = LightColors.OnSurface,
    surfaceVariant = LightColors.SurfaceVariant,
    onSurfaceVariant = LightColors.OnSurfaceVariant,
    error = LightColors.Error,
    onError = LightColors.OnError,
    outline = LightColors.Outline
)

/**
 * Color scheme for Ocean theme (Subtle Dark Blue)
 */
private val OceanColorScheme = darkColorScheme(
    primary = OceanColors.Primary,
    onPrimary = OceanColors.OnPrimary,
    primaryContainer = OceanColors.PrimaryContainer,
    onPrimaryContainer = OceanColors.OnPrimaryContainer,
    secondary = OceanColors.Secondary,
    onSecondary = OceanColors.OnSecondary,
    secondaryContainer = OceanColors.SecondaryContainer,
    onSecondaryContainer = OceanColors.OnSecondaryContainer,
    tertiary = OceanColors.Tertiary,
    onTertiary = OceanColors.OnTertiary,
    tertiaryContainer = OceanColors.TertiaryContainer,
    onTertiaryContainer = OceanColors.OnTertiaryContainer,
    background = OceanColors.Background,
    onBackground = OceanColors.OnBackground,
    surface = OceanColors.Surface,
    onSurface = OceanColors.OnSurface,
    surfaceVariant = OceanColors.SurfaceVariant,
    onSurfaceVariant = OceanColors.OnSurfaceVariant,
    error = OceanColors.Error,
    onError = OceanColors.OnError,
    outline = OceanColors.Outline
)

/**
 * Color scheme for Rosewood theme (Subtle Warm)
 */
private val RosewoodColorScheme = darkColorScheme(
    primary = RosewoodColors.Primary,
    onPrimary = RosewoodColors.OnPrimary,
    primaryContainer = RosewoodColors.PrimaryContainer,
    onPrimaryContainer = RosewoodColors.OnPrimaryContainer,
    secondary = RosewoodColors.Secondary,
    onSecondary = RosewoodColors.OnSecondary,
    secondaryContainer = RosewoodColors.SecondaryContainer,
    onSecondaryContainer = RosewoodColors.OnSecondaryContainer,
    tertiary = RosewoodColors.Tertiary,
    onTertiary = RosewoodColors.OnTertiary,
    tertiaryContainer = RosewoodColors.TertiaryContainer,
    onTertiaryContainer = RosewoodColors.OnTertiaryContainer,
    background = RosewoodColors.Background,
    onBackground = RosewoodColors.OnBackground,
    surface = RosewoodColors.Surface,
    onSurface = RosewoodColors.OnSurface,
    surfaceVariant = RosewoodColors.SurfaceVariant,
    onSurfaceVariant = RosewoodColors.OnSurfaceVariant,
    error = RosewoodColors.Error,
    onError = RosewoodColors.OnError,
    outline = RosewoodColors.Outline
)

/**
 * Color scheme for Blue Sky theme
 */
private val BlueSkyColorScheme = darkColorScheme(
    primary = BlueSkyColors.Primary,
    onPrimary = BlueSkyColors.OnPrimary,
    primaryContainer = BlueSkyColors.PrimaryContainer,
    onPrimaryContainer = BlueSkyColors.OnPrimaryContainer,
    secondary = BlueSkyColors.Secondary,
    onSecondary = BlueSkyColors.OnSecondary,
    secondaryContainer = BlueSkyColors.SecondaryContainer,
    onSecondaryContainer = BlueSkyColors.OnSecondaryContainer,
    tertiary = BlueSkyColors.Tertiary,
    onTertiary = BlueSkyColors.OnTertiary,
    tertiaryContainer = BlueSkyColors.TertiaryContainer,
    onTertiaryContainer = BlueSkyColors.OnTertiaryContainer,
    background = BlueSkyColors.Background,
    onBackground = BlueSkyColors.OnBackground,
    surface = BlueSkyColors.Surface,
    onSurface = BlueSkyColors.OnSurface,
    surfaceVariant = BlueSkyColors.SurfaceVariant,
    onSurfaceVariant = BlueSkyColors.OnSurfaceVariant,
    error = BlueSkyColors.Error,
    onError = BlueSkyColors.OnError,
    outline = BlueSkyColors.Outline
)

/**
 * App typography
 */
private val AppTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * Get color scheme based on theme
 */
fun getColorScheme(theme: AppTheme): ColorScheme {
    return when (theme) {
        AppTheme.MIDNIGHT -> MidnightColorScheme
        AppTheme.LIGHT -> LightColorScheme
        AppTheme.OCEAN -> OceanColorScheme
        AppTheme.ROSEWOOD -> RosewoodColorScheme
        AppTheme.BLUE_SKY -> BlueSkyColorScheme
    }
}

/**
 * Main theme composable
 */
@Composable
fun RJournalTheme(
    theme: AppTheme = AppTheme.MIDNIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = getColorScheme(theme)

    CompositionLocalProvider(LocalAppTheme provides theme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}