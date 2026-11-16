package com.baverika.r_journal.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================
// THEME: MIDNIGHT (Dark - Pitch Black OLED)
// True pitch black theme with subtle dark charcoal supporting layers
// ============================================
object MidnightColors {
    val Primary = Color(0xFFD6D6D6)          // Near white / soft light grey
    val OnPrimary = Color(0xFF0A0A0A)        // Pitch black-adjacent dark grey
    val PrimaryContainer = Color(0xFF222222) // Dark charcoal
    val OnPrimaryContainer = Color(0xFFD6D6D6) // Near white / soft light grey

    val Secondary = Color(0xFFB0B0B0)        // Muted medium-light grey
    val OnSecondary = Color(0xFF0A0A0A)
    val SecondaryContainer = Color(0xFF181818)
    val OnSecondaryContainer = Color(0xFFCCCCCC)

    val Tertiary = Color(0xFF888888)         // Medium grey
    val OnTertiary = Color(0xFF0A0A0A)
    val TertiaryContainer = Color(0xFF1C1C1C)
    val OnTertiaryContainer = Color(0xFFB0B0B0)

    val Background = Color(0xFF000000)       // Pitch black OLED
    val OnBackground = Color(0xFFD6D6D6)     // Near white / soft light grey
    val Surface = Color(0xFF0C0C0C)          // Slightly lighter dark grey for contrast
    val OnSurface = Color(0xFFD6D6D6)        // Near white / soft light grey
    val SurfaceVariant = Color(0xFF161616)   // Medium-dark grey for cards/containers
    val OnSurfaceVariant = Color(0xFF9E9E9E) // Muted text / placeholders

    val Error = Color(0xFFE5A3A3)            // Muted, desaturated red for errors to fit monochrome aesthetic
    val OnError = Color(0xFF4E0010)
    val Outline = Color(0xFF2C2C2C)          // Divider and border line grey
}

// ============================================
// THEME: LIGHT
// Clean, bright theme
// ============================================
object LightColors {
    val Primary = Color(0xFF0F766E)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFA8F2D6)
    val OnPrimaryContainer = Color(0xFF002117)

    val Secondary = Color(0xFF4F6352)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFD2E8D2)
    val OnSecondaryContainer = Color(0xFF0D1F12)

    val Tertiary = Color(0xFF3D6373)
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFC1E8FB)
    val OnTertiaryContainer = Color(0xFF001F2A)

    val Background = Color(0xFFFCFDF7)
    val OnBackground = Color(0xFF191C19)
    val Surface = Color(0xFFFFFFFF)
    val OnSurface = Color(0xFF191C19)
    val SurfaceVariant = Color(0xFFDDE5DB)
    val OnSurfaceVariant = Color(0xFF414941)

    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val Outline = Color(0xFF717971)
}

// ============================================
// THEME: OCEAN (Subtle Dark Blue)
// Calm, deep blue tones - easy on the eyes
// ============================================
object OceanColors {
    val Primary = Color(0xFF7DD3FC) // Sky blue
    val OnPrimary = Color(0xFF003548)
    val PrimaryContainer = Color(0xFF004D67)
    val OnPrimaryContainer = Color(0xFFBFE9FF)

    val Secondary = Color(0xFFB4CAD6)
    val OnSecondary = Color(0xFF1F333C)
    val SecondaryContainer = Color(0xFF354A53)
    val OnSecondaryContainer = Color(0xFFD0E6F2)

    val Tertiary = Color(0xFFC5C3EA)
    val OnTertiary = Color(0xFF2E2D4D)
    val TertiaryContainer = Color(0xFF454364)
    val OnTertiaryContainer = Color(0xFFE3DFFF)

    val Background = Color(0xFF0C1821) // Deep navy
    val OnBackground = Color(0xFFDEE3E7)
    val Surface = Color(0xFF111D28)
    val OnSurface = Color(0xFFDEE3E7)
    val SurfaceVariant = Color(0xFF1A2C3A)
    val OnSurfaceVariant = Color(0xFFC0C8CD)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val Outline = Color(0xFF4A5C67)
}

// ============================================
// THEME: SPACE (Pure Black OLED Glassmorphism)
// Redesigned from Rosewood: Pure black OLED (#000000), near-black (#050505),
// translucent charcoal glass surfaces, 1px subtle white borders,
// neutral silver/soft white accents, and clean monochrome contrast.
// ============================================
object RosewoodColors {
    // Primary: Neutral silver / soft white accent
    val Primary = Color(0xFFEDEDED)          // Crisp neutral silver
    val OnPrimary = Color(0xFF000000)        // Pure black
    val PrimaryContainer = Color(0xFF1E2026) // Translucent dark silver-charcoal container
    val OnPrimaryContainer = Color(0xFFF3F4F6) // Pure soft white

    // Secondary: Muted silver / cool grey
    val Secondary = Color(0xFFC5C8D0)        // Silver grey
    val OnSecondary = Color(0xFF0A0A0A)
    val SecondaryContainer = Color(0xFF16181D) // Charcoal glass container
    val OnSecondaryContainer = Color(0xFFE5E7EB)

    // Tertiary: Platinum slate accent
    val Tertiary = Color(0xFFA1A7B4)         // Platinum slate
    val OnTertiary = Color(0xFF0A0A0A)
    val TertiaryContainer = Color(0xFF1A1C21)
    val OnTertiaryContainer = Color(0xFFD1D5DB)

    // Backgrounds: Pure Black OLED & Near-Black
    val Background = Color(0xFF000000)       // Pure black (#000000)
    val OnBackground = Color(0xFFF3F4F6)     // White primary text
    val SecondaryBackground = Color(0xFF050505) // Near-black (#050505)

    // Glass Surfaces: Translucent charcoal
    val Surface = Color(0xFF090A0D)          // Deep charcoal glass surface
    val OnSurface = Color(0xFFF3F4F6)        // White primary text
    val SurfaceVariant = Color(0xFF13151A)   // Elevated glass card surface
    val OnSurfaceVariant = Color(0xFF9CA3AF) // Muted grey secondary text

    // Glass Borders: Subtle translucent white (low opacity)
    val Outline = Color(0x33FFFFFF)          // 20% white 1px border
    val OutlineVariant = Color(0x1AFFFFFF)   // 10% white very subtle border

    // Error: Refined muted crimson
    val Error = Color(0xFFF87171)
    val OnError = Color(0xFF450A0A)
}

// ============================================
// THEME: BLUE SKY (Celestial)
// Deep space blue with gold accents
// ============================================
object BlueSkyColors {
    val Primary = Color(0xFFFFD700) // Gold
    val OnPrimary = Color(0xFF3E1C00)
    val PrimaryContainer = Color(0xFF5C4000)
    val OnPrimaryContainer = Color(0xFFFFE0A0)

    val Secondary = Color(0xFFB0C6FF)
    val OnSecondary = Color(0xFF002D6C)
    val SecondaryContainer = Color(0xFF19438F)
    val OnSecondaryContainer = Color(0xFFD9E2FF)

    val Tertiary = Color(0xFFC4C0FF)
    val OnTertiary = Color(0xFF2B285E)
    val TertiaryContainer = Color(0xFF434077)
    val OnTertiaryContainer = Color(0xFFE2DFFF)

    val Background = Color(0xFF051025) // Deep Space Blue
    val OnBackground = Color(0xFFE2E2E6)
    val Surface = Color(0xFF0B1A35) // Slightly lighter blue
    val OnSurface = Color(0xFFE2E2E6)
    val SurfaceVariant = Color(0xFF152545)
    val OnSurfaceVariant = Color(0xFFC4C6D0)

    val Error = Color(0xFFFFB4AB)
    val OnError = Color(0xFF690005)
    val Outline = Color(0xFF8E9099)
}

// ============================================
// THEME: CLOUD DANCER (Pantone Color of the Year 2026)
// Warm, creamy off-white inspired by PANTONE 11-4201
// Elegant light theme — not pure white, but a living canvas
// ============================================
object CloudDancerColors {
    // Primary accent: warm sage green — earthy and grounding
    val Primary = Color(0xFF6B8C6F)         // Sage Green
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFCDE8C8) // Pale sage
    val OnPrimaryContainer = Color(0xFF1A3320)

    // Secondary: warm dusty rose — feminine and soft
    val Secondary = Color(0xFFA07870)        // Dusty rose
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFEED9D5) // Blush
    val OnSecondaryContainer = Color(0xFF3B1917)

    // Tertiary: warm terracotta sand
    val Tertiary = Color(0xFFAA8865)         // Warm sand / terracotta
    val OnTertiary = Color(0xFFFFFFFF)
    val TertiaryContainer = Color(0xFFEFDCC8) // Pale sand
    val OnTertiaryContainer = Color(0xFF3C2410)

    // Background: Pantone Cloud Dancer (#F2F0E9) — warm off-white, NOT pure white
    val Background = Color(0xFFF2F0E9)
    val OnBackground = Color(0xFF2C2520)     // Warm dark brown
    val Surface = Color(0xFFFAF8F3)          // Slightly lighter warm white
    val OnSurface = Color(0xFF2C2520)
    val SurfaceVariant = Color(0xFFE8E4DA)   // Warm light grey-tan
    val OnSurfaceVariant = Color(0xFF5C5249)

    val Error = Color(0xFFB3261E)
    val OnError = Color(0xFFFFFFFF)
    val Outline = Color(0xFFB0A89E)          // Warm greige
}

// Theme enum for easy reference
enum class AppTheme(val displayName: String) {
    MIDNIGHT("Midnight"),
    LIGHT("Light"),
    OCEAN("Ocean"),
    ROSEWOOD("Space"),
    BLUE_SKY("Blue Sky"),
    CLOUD_DANCER("Cloud '26");

    val isDark: Boolean
        get() = this != LIGHT && this != CLOUD_DANCER
}

