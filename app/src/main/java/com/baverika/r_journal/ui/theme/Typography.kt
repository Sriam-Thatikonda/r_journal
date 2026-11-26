package com.baverika.r_journal.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.baverika.r_journal.R

enum class AppFontStyle(
    val displayName: String,
    val subtitle: String,
    val sampleHeadline: String
) {
    MODERN(
        displayName = "Modern Plus",
        subtitle = "Plus Jakarta Sans — geometric clarity & warmth",
        sampleHeadline = "Capture today's quiet moments"
    ),
    EDITORIAL(
        displayName = "Editorial",
        subtitle = "Instrument Serif titles paired with Inter body",
        sampleHeadline = "Reflections written in calm ink"
    ),
    CRISP(
        displayName = "Crisp Sans",
        subtitle = "Inter precision with JetBrains Mono accents",
        sampleHeadline = "Structured clarity for daily focus"
    );

    companion object {
        fun fromPersistedName(name: String?): AppFontStyle {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MODERN
        }
    }
}

val LocalAppFontStyle = staticCompositionLocalOf { AppFontStyle.MODERN }

val PlusJakartaSansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val InstrumentSerifFamily = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_regular, FontWeight.Medium),
    Font(R.font.instrument_serif_regular, FontWeight.SemiBold),
    Font(R.font.instrument_serif_regular, FontWeight.Bold),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic)
)

val JetBrainsMonoFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold)
)

fun buildAppTypography(style: AppFontStyle): Typography {
    val displayFamily: FontFamily
    val uiFamily: FontFamily

    when (style) {
        AppFontStyle.MODERN -> {
            displayFamily = PlusJakartaSansFamily
            uiFamily = PlusJakartaSansFamily
        }
        AppFontStyle.EDITORIAL -> {
            displayFamily = InstrumentSerifFamily
            uiFamily = InterFamily
        }
        AppFontStyle.CRISP -> {
            displayFamily = InterFamily
            uiFamily = InterFamily
        }
    }

    val isEditorial = style == AppFontStyle.EDITORIAL

    return Typography(
        displayLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = if (isEditorial) FontWeight.Normal else FontWeight.Bold,
            fontSize = if (isEditorial) 56.sp else 52.sp,
            lineHeight = if (isEditorial) 62.sp else 58.sp,
            letterSpacing = (-0.5).sp
        ),
        displayMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = if (isEditorial) FontWeight.Normal else FontWeight.Bold,
            fontSize = if (isEditorial) 44.sp else 40.sp,
            lineHeight = if (isEditorial) 50.sp else 46.sp,
            letterSpacing = (-0.4).sp
        ),
        displaySmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = if (isEditorial) FontWeight.Normal else FontWeight.SemiBold,
            fontSize = if (isEditorial) 36.sp else 32.sp,
            lineHeight = if (isEditorial) 42.sp else 38.sp,
            letterSpacing = (-0.25).sp
        ),
        headlineLarge = TextStyle(
            fontFamily = displayFamily,
            fontWeight = if (isEditorial) FontWeight.Normal else FontWeight.Bold,
            fontSize = if (isEditorial) 32.sp else 28.sp,
            lineHeight = if (isEditorial) 38.sp else 34.sp,
            letterSpacing = (-0.2).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = displayFamily,
            fontWeight = if (isEditorial) FontWeight.Normal else FontWeight.SemiBold,
            fontSize = if (isEditorial) 28.sp else 24.sp,
            lineHeight = if (isEditorial) 34.sp else 30.sp,
            letterSpacing = (-0.15).sp
        ),
        headlineSmall = TextStyle(
            fontFamily = displayFamily,
            fontWeight = if (isEditorial) FontWeight.Normal else FontWeight.SemiBold,
            fontSize = if (isEditorial) 24.sp else 21.sp,
            lineHeight = if (isEditorial) 30.sp else 27.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
            letterSpacing = (-0.15).sp
        ),
        titleMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp
        ),
        titleSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.05.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.5.sp,
            lineHeight = 23.sp,
            letterSpacing = 0.1.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            letterSpacing = 0.2.sp
        ),
        labelLarge = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.15.sp
        ),
        labelMedium = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.35.sp
        ),
        labelSmall = TextStyle(
            fontFamily = uiFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.5.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.45.sp
        )
    )
}
