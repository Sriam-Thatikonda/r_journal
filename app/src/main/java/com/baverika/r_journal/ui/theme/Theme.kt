package com.baverika.r_journal.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * A custom, eye-friendly dark color scheme based on Material 3's baseline.
 *
 * Adjustments made:
 * - Slightly warmer background tones (blue-grey instead of pure grey) to reduce harshness.
 * - Ensured good contrast for primary/accent colors.
 * - Used tonal elevation for surfaces.
 */
private val CustomDarkColorScheme = darkColorScheme(
    // Primary: A vibrant, saturated color for key actions/elements.
    primary = Color(0xFF8FBFA8),
    onPrimary = Color(0xFF073B34),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),

    // Secondary: A complementary color, often for less prominent actions.
    secondary = Color(0xFFE0CDA9),
    onSecondary = Color(0xFF3A2E1D),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),

    // Tertiary: Often for decorative or alternate actions.
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),

    // Background: Pitch black as requested
    background = Color.Black,
    onBackground = Color(0xFFE6E1D8), // High contrast text

    // Surface: Elements that sit on top of the background (cards, sheets).
    surface = Color.Black,
    onSurface = Color(0xFFCAC4D0), // Slightly lower contrast than background text
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    // Error: For error states and destructive actions.
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    // Outline: For borders and dividers.
    outline = Color(0xFF938F99),

    // Inverse colors (used for light-on-dark scenarios, less common in pure dark themes)
    inverseOnSurface = Color(0xFF1C1B1F),
    inverseSurface = Color(0xFFE6E1E5),
    inversePrimary = Color(0xFF6750A4),

    // Scrim (for modals, usually semi-transparent black)
    scrim = Color(0xFF000000).copy(alpha = 0.32f)
)

/**
 * Define your app's typography.
 * This ensures consistency in text styles across the app.
 */
private val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Add other text styles as needed (titleLarge, labelMedium, etc.)
    // You can customize these based on your design.
)

/**
 * The main composable function that wraps your app content with the MaterialTheme.
 *
 * @param darkTheme Whether to use the dark theme. Defaults to system setting.
 * @param dynamicColor Whether to use dynamic colors from the device (Android 12+).
 *                      Falls back to the custom scheme if not supported.
 * @param content The main content of your app.
 */
@Composable
fun RJournalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context).copy(background = Color.Black, surface = Color.Black) else dynamicLightColorScheme(context)
        }

        darkTheme -> CustomDarkColorScheme
        else -> lightColorScheme() // Or define a CustomLightColorScheme if desired
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Use our defined Typography
        content = content
    )
}