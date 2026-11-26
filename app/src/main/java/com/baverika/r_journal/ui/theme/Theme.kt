package com.baverika.r_journal.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalAppTheme = staticCompositionLocalOf { AppTheme.OBSIDIAN }

/**
 * Helper to convert vibrant habit/tag colors into sleek monochromatic grayscale shades
 * when Obsidian OLED theme is active.
 */
fun getEffectiveColor(color: Color, theme: AppTheme): Color {
    return if (theme == AppTheme.OBSIDIAN) {
        val brightness = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f)
        val gray = 0.55f + (brightness * 0.33f)
        Color(gray, gray, gray, color.alpha)
    } else {
        color
    }
}

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianPalette.Primary,
    onPrimary = ObsidianPalette.OnPrimary,
    primaryContainer = ObsidianPalette.PrimaryContainer,
    onPrimaryContainer = ObsidianPalette.OnPrimaryContainer,
    secondary = ObsidianPalette.Secondary,
    onSecondary = ObsidianPalette.OnSecondary,
    secondaryContainer = ObsidianPalette.SecondaryContainer,
    onSecondaryContainer = ObsidianPalette.OnSecondaryContainer,
    tertiary = ObsidianPalette.Tertiary,
    onTertiary = ObsidianPalette.OnTertiary,
    tertiaryContainer = ObsidianPalette.TertiaryContainer,
    onTertiaryContainer = ObsidianPalette.OnTertiaryContainer,
    background = ObsidianPalette.Background,
    onBackground = ObsidianPalette.OnBackground,
    surface = ObsidianPalette.Surface,
    onSurface = ObsidianPalette.OnSurface,
    surfaceVariant = ObsidianPalette.SurfaceVariant,
    onSurfaceVariant = ObsidianPalette.OnSurfaceVariant,
    surfaceContainerLowest = ObsidianPalette.SurfaceContainerLowest,
    surfaceContainerLow = ObsidianPalette.SurfaceContainerLow,
    surfaceContainer = ObsidianPalette.SurfaceContainer,
    surfaceContainerHigh = ObsidianPalette.SurfaceContainerHigh,
    surfaceContainerHighest = ObsidianPalette.SurfaceContainerHighest,
    error = ObsidianPalette.Error,
    onError = ObsidianPalette.OnError,
    outline = ObsidianPalette.Outline,
    outlineVariant = ObsidianPalette.OutlineVariant,
    surfaceTint = Color.Transparent
)

private val IvoryColorScheme = lightColorScheme(
    primary = IvoryPalette.Primary,
    onPrimary = IvoryPalette.OnPrimary,
    primaryContainer = IvoryPalette.PrimaryContainer,
    onPrimaryContainer = IvoryPalette.OnPrimaryContainer,
    secondary = IvoryPalette.Secondary,
    onSecondary = IvoryPalette.OnSecondary,
    secondaryContainer = IvoryPalette.SecondaryContainer,
    onSecondaryContainer = IvoryPalette.OnSecondaryContainer,
    tertiary = IvoryPalette.Tertiary,
    onTertiary = IvoryPalette.OnTertiary,
    tertiaryContainer = IvoryPalette.TertiaryContainer,
    onTertiaryContainer = IvoryPalette.OnTertiaryContainer,
    background = IvoryPalette.Background,
    onBackground = IvoryPalette.OnBackground,
    surface = IvoryPalette.Surface,
    onSurface = IvoryPalette.OnSurface,
    surfaceVariant = IvoryPalette.SurfaceVariant,
    onSurfaceVariant = IvoryPalette.OnSurfaceVariant,
    surfaceContainerLowest = IvoryPalette.SurfaceContainerLowest,
    surfaceContainerLow = IvoryPalette.SurfaceContainerLow,
    surfaceContainer = IvoryPalette.SurfaceContainer,
    surfaceContainerHigh = IvoryPalette.SurfaceContainerHigh,
    surfaceContainerHighest = IvoryPalette.SurfaceContainerHighest,
    error = IvoryPalette.Error,
    onError = IvoryPalette.OnError,
    outline = IvoryPalette.Outline,
    outlineVariant = IvoryPalette.OutlineVariant,
    surfaceTint = Color.Transparent
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPalette.Primary,
    onPrimary = OceanPalette.OnPrimary,
    primaryContainer = OceanPalette.PrimaryContainer,
    onPrimaryContainer = OceanPalette.OnPrimaryContainer,
    secondary = OceanPalette.Secondary,
    onSecondary = OceanPalette.OnSecondary,
    secondaryContainer = OceanPalette.SecondaryContainer,
    onSecondaryContainer = OceanPalette.OnSecondaryContainer,
    tertiary = OceanPalette.Tertiary,
    onTertiary = OceanPalette.OnTertiary,
    tertiaryContainer = OceanPalette.TertiaryContainer,
    onTertiaryContainer = OceanPalette.OnTertiaryContainer,
    background = OceanPalette.Background,
    onBackground = OceanPalette.OnBackground,
    surface = OceanPalette.Surface,
    onSurface = OceanPalette.OnSurface,
    surfaceVariant = OceanPalette.SurfaceVariant,
    onSurfaceVariant = OceanPalette.OnSurfaceVariant,
    surfaceContainerLowest = OceanPalette.SurfaceContainerLowest,
    surfaceContainerLow = OceanPalette.SurfaceContainerLow,
    surfaceContainer = OceanPalette.SurfaceContainer,
    surfaceContainerHigh = OceanPalette.SurfaceContainerHigh,
    surfaceContainerHighest = OceanPalette.SurfaceContainerHighest,
    error = OceanPalette.Error,
    onError = OceanPalette.OnError,
    outline = OceanPalette.Outline,
    outlineVariant = OceanPalette.OutlineVariant,
    surfaceTint = Color.Transparent
)

private val SpaceGlassColorScheme = darkColorScheme(
    primary = SpaceGlassPalette.Primary,
    onPrimary = SpaceGlassPalette.OnPrimary,
    primaryContainer = SpaceGlassPalette.PrimaryContainer,
    onPrimaryContainer = SpaceGlassPalette.OnPrimaryContainer,
    secondary = SpaceGlassPalette.Secondary,
    onSecondary = SpaceGlassPalette.OnSecondary,
    secondaryContainer = SpaceGlassPalette.SecondaryContainer,
    onSecondaryContainer = SpaceGlassPalette.OnSecondaryContainer,
    tertiary = SpaceGlassPalette.Tertiary,
    onTertiary = SpaceGlassPalette.OnTertiary,
    tertiaryContainer = SpaceGlassPalette.TertiaryContainer,
    onTertiaryContainer = SpaceGlassPalette.OnTertiaryContainer,
    background = SpaceGlassPalette.Background,
    onBackground = SpaceGlassPalette.OnBackground,
    surface = SpaceGlassPalette.Surface,
    onSurface = SpaceGlassPalette.OnSurface,
    surfaceVariant = SpaceGlassPalette.SurfaceVariant,
    onSurfaceVariant = SpaceGlassPalette.OnSurfaceVariant,
    surfaceContainerLowest = SpaceGlassPalette.SurfaceContainerLowest,
    surfaceContainerLow = SpaceGlassPalette.SurfaceContainerLow,
    surfaceContainer = SpaceGlassPalette.SurfaceContainer,
    surfaceContainerHigh = SpaceGlassPalette.SurfaceContainerHigh,
    surfaceContainerHighest = SpaceGlassPalette.SurfaceContainerHighest,
    error = SpaceGlassPalette.Error,
    onError = SpaceGlassPalette.OnError,
    outline = SpaceGlassPalette.Outline,
    outlineVariant = SpaceGlassPalette.OutlineVariant,
    surfaceTint = Color.Transparent,
    scrim = Color(0xFF000000)
)

fun getColorScheme(theme: AppTheme): ColorScheme {
    return when (theme) {
        AppTheme.OBSIDIAN -> ObsidianColorScheme
        AppTheme.IVORY -> IvoryColorScheme
        AppTheme.OCEAN -> OceanColorScheme
        AppTheme.SPACE -> SpaceGlassColorScheme
    }
}

@Composable
private fun animatedColor(target: Color): Color {
    val color by animateColorAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
        label = "themeColorMorph"
    )
    return color
}

@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme): ColorScheme {
    return target.copy(
        primary = animatedColor(target.primary),
        onPrimary = animatedColor(target.onPrimary),
        primaryContainer = animatedColor(target.primaryContainer),
        onPrimaryContainer = animatedColor(target.onPrimaryContainer),
        secondary = animatedColor(target.secondary),
        onSecondary = animatedColor(target.onSecondary),
        secondaryContainer = animatedColor(target.secondaryContainer),
        onSecondaryContainer = animatedColor(target.onSecondaryContainer),
        tertiary = animatedColor(target.tertiary),
        onTertiary = animatedColor(target.onTertiary),
        tertiaryContainer = animatedColor(target.tertiaryContainer),
        onTertiaryContainer = animatedColor(target.onTertiaryContainer),
        background = animatedColor(target.background),
        onBackground = animatedColor(target.onBackground),
        surface = animatedColor(target.surface),
        onSurface = animatedColor(target.onSurface),
        surfaceVariant = animatedColor(target.surfaceVariant),
        onSurfaceVariant = animatedColor(target.onSurfaceVariant),
        surfaceContainerLowest = animatedColor(target.surfaceContainerLowest),
        surfaceContainerLow = animatedColor(target.surfaceContainerLow),
        surfaceContainer = animatedColor(target.surfaceContainer),
        surfaceContainerHigh = animatedColor(target.surfaceContainerHigh),
        surfaceContainerHighest = animatedColor(target.surfaceContainerHighest),
        error = animatedColor(target.error),
        onError = animatedColor(target.onError),
        outline = animatedColor(target.outline),
        outlineVariant = animatedColor(target.outlineVariant)
    )
}

@Composable
fun RJournalTheme(
    theme: AppTheme = AppTheme.OBSIDIAN,
    fontStyle: AppFontStyle = AppFontStyle.MODERN,
    content: @Composable () -> Unit
) {
    val targetScheme = remember(theme) { getColorScheme(theme) }
    val colorScheme = rememberAnimatedColorScheme(targetScheme)
    val typography = remember(fontStyle) { buildAppTypography(fontStyle) }
    val tokens = remember(theme) { tokensForTheme(theme) }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                val isLight = !theme.isDark
                insetsController.isAppearanceLightStatusBars = isLight
                insetsController.isAppearanceLightNavigationBars = isLight
            }
        }
    }

    CompositionLocalProvider(
        LocalAppTheme provides theme,
        LocalAppFontStyle provides fontStyle,
        LocalDesignTokens provides tokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
