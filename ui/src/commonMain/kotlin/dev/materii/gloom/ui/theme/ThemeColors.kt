package dev.materii.gloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.materii.gloom.domain.manager.enums.ColorTheme

// ─── GitHub ───────────────────────────────────────────────────────────────────
// Accent: GitHub Blue (#0969DA light / #58A6FF dark)

private val GitHubLightScheme = lightColorScheme(
    primary               = Color(0xFF0969DA),
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Color(0xFFDAEAFF),
    onPrimaryContainer    = Color(0xFF002D68),
    secondary             = Color(0xFF1A7F37),
    onSecondary           = Color(0xFFFFFFFF),
    secondaryContainer    = Color(0xFFD4EDDA),
    onSecondaryContainer  = Color(0xFF0D3B1E),
    tertiary              = Color(0xFF8250DF),
    onTertiary            = Color(0xFFFFFFFF),
    tertiaryContainer     = Color(0xFFEDD9FF),
    onTertiaryContainer   = Color(0xFF2E0F63),
)

private val GitHubDarkScheme = darkColorScheme(
    primary               = Color(0xFF58A6FF),
    onPrimary             = Color(0xFF00205E),
    primaryContainer      = Color(0xFF003389),
    onPrimaryContainer    = Color(0xFFAECFFF),
    secondary             = Color(0xFF3FB950),
    onSecondary           = Color(0xFF003D12),
    secondaryContainer    = Color(0xFF00581C),
    onSecondaryContainer  = Color(0xFF96E4A5),
    tertiary              = Color(0xFFD2A8FF),
    onTertiary            = Color(0xFF3B0D78),
    tertiaryContainer     = Color(0xFF5A22B8),
    onTertiaryContainer   = Color(0xFFEDD9FF),
)

// ─── Catppuccin ───────────────────────────────────────────────────────────────
// Dark = Mocha (mauve #CBA6F7), Light = Latte (mauve #8839EF)

private val CatppuccinLightScheme = lightColorScheme(
    primary               = Color(0xFF8839EF),
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Color(0xFFEEDAFF),
    onPrimaryContainer    = Color(0xFF30006D),
    secondary             = Color(0xFF179299),
    onSecondary           = Color(0xFFFFFFFF),
    secondaryContainer    = Color(0xFFBBF0F3),
    onSecondaryContainer  = Color(0xFF002B2D),
    tertiary              = Color(0xFFDF8E1D),
    onTertiary            = Color(0xFFFFFFFF),
    tertiaryContainer     = Color(0xFFFFDFA8),
    onTertiaryContainer   = Color(0xFF2D1600),
    background            = Color(0xFFEFF1F5),
    onBackground          = Color(0xFF4C4F69),
    surface               = Color(0xFFE6E9EF),
    onSurface             = Color(0xFF4C4F69),
)

private val CatppuccinDarkScheme = darkColorScheme(
    primary               = Color(0xFFCBA6F7),
    onPrimary             = Color(0xFF3A0070),
    primaryContainer      = Color(0xFF5500A8),
    onPrimaryContainer    = Color(0xFFEDDAFF),
    secondary             = Color(0xFF89DCEB),
    onSecondary           = Color(0xFF003037),
    secondaryContainer    = Color(0xFF004F5A),
    onSecondaryContainer  = Color(0xFFBBF0F3),
    tertiary              = Color(0xFFF9E2AF),
    onTertiary            = Color(0xFF3D2B00),
    tertiaryContainer     = Color(0xFF5A3F00),
    onTertiaryContainer   = Color(0xFFFFDFA8),
    background            = Color(0xFF1E1E2E),
    onBackground          = Color(0xFFCDD6F4),
    surface               = Color(0xFF181825),
    onSurface             = Color(0xFFCDD6F4),
    surfaceVariant        = Color(0xFF313244),
    onSurfaceVariant      = Color(0xFFBAC2DE),
)

// ─── Dracula ──────────────────────────────────────────────────────────────────
// Accent: purple #BD93F9 dark / #6272A4 light

private val DraculaLightScheme = lightColorScheme(
    primary               = Color(0xFF6272A4),
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Color(0xFFDADEF5),
    onPrimaryContainer    = Color(0xFF1A2050),
    secondary             = Color(0xFF2DA44E),
    onSecondary           = Color(0xFFFFFFFF),
    secondaryContainer    = Color(0xFFD4EDDA),
    onSecondaryContainer  = Color(0xFF0D3B1E),
    tertiary              = Color(0xFFFF79C6),
    onTertiary            = Color(0xFFFFFFFF),
    tertiaryContainer     = Color(0xFFFFD9EE),
    onTertiaryContainer   = Color(0xFF3E0028),
    background            = Color(0xFFF8F8F2),
    onBackground          = Color(0xFF282A36),
)

private val DraculaDarkScheme = darkColorScheme(
    primary               = Color(0xFFBD93F9),
    onPrimary             = Color(0xFF2A0060),
    primaryContainer      = Color(0xFF44009A),
    onPrimaryContainer    = Color(0xFFEADDFF),
    secondary             = Color(0xFF50FA7B),
    onSecondary           = Color(0xFF00391A),
    secondaryContainer    = Color(0xFF005228),
    onSecondaryContainer  = Color(0xFF96F5AD),
    tertiary              = Color(0xFFFF79C6),
    onTertiary            = Color(0xFF5C0036),
    tertiaryContainer     = Color(0xFF7D0050),
    onTertiaryContainer   = Color(0xFFFFD9EE),
    background            = Color(0xFF282A36),
    onBackground          = Color(0xFFF8F8F2),
    surface               = Color(0xFF21222C),
    onSurface             = Color(0xFFF8F8F2),
    surfaceVariant        = Color(0xFF44475A),
    onSurfaceVariant      = Color(0xFFCFD0D9),
)

// ─── Nord ─────────────────────────────────────────────────────────────────────
// Accent: #88C0D0 dark / #5E81AC light

private val NordLightScheme = lightColorScheme(
    primary               = Color(0xFF5E81AC),
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Color(0xFFD8E4F5),
    onPrimaryContainer    = Color(0xFF1A2D47),
    secondary             = Color(0xFF81A1C1),
    onSecondary           = Color(0xFFFFFFFF),
    secondaryContainer    = Color(0xFFDCEBF7),
    onSecondaryContainer  = Color(0xFF1C3347),
    tertiary              = Color(0xFF88C0D0),
    onTertiary            = Color(0xFFFFFFFF),
    tertiaryContainer     = Color(0xFFD8EFF4),
    onTertiaryContainer   = Color(0xFF003844),
    background            = Color(0xFFECEFF4),
    onBackground          = Color(0xFF2E3440),
    surface               = Color(0xFFE5E9F0),
    onSurface             = Color(0xFF2E3440),
)

private val NordDarkScheme = darkColorScheme(
    primary               = Color(0xFF88C0D0),
    onPrimary             = Color(0xFF003845),
    primaryContainer      = Color(0xFF005262),
    onPrimaryContainer    = Color(0xFFB8E4EE),
    secondary             = Color(0xFF81A1C1),
    onSecondary           = Color(0xFF1C3347),
    secondaryContainer    = Color(0xFF2E4A64),
    onSecondaryContainer  = Color(0xFFBDD4E8),
    tertiary              = Color(0xFF5E81AC),
    onTertiary            = Color(0xFF1A2D47),
    tertiaryContainer     = Color(0xFF2D4462),
    onTertiaryContainer   = Color(0xFFD8E4F5),
    background            = Color(0xFF2E3440),
    onBackground          = Color(0xFFECEFF4),
    surface               = Color(0xFF3B4252),
    onSurface             = Color(0xFFECEFF4),
    surfaceVariant        = Color(0xFF434C5E),
    onSurfaceVariant      = Color(0xFFD8DEE9),
)

// ─── Rosé Pine ────────────────────────────────────────────────────────────────
// Accent: #EBBCBA dark / #B4637A light

private val RosePineLightScheme = lightColorScheme(
    primary               = Color(0xFFB4637A),
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Color(0xFFFFD9E2),
    onPrimaryContainer    = Color(0xFF3E001F),
    secondary             = Color(0xFF907AA9),
    onSecondary           = Color(0xFFFFFFFF),
    secondaryContainer    = Color(0xFFEFDBFF),
    onSecondaryContainer  = Color(0xFF2A0047),
    tertiary              = Color(0xFF56949F),
    onTertiary            = Color(0xFFFFFFFF),
    tertiaryContainer     = Color(0xFFBFE8ED),
    onTertiaryContainer   = Color(0xFF001F24),
    background            = Color(0xFFFAF4ED),
    onBackground          = Color(0xFF575279),
    surface               = Color(0xFFF2E9E1),
    onSurface             = Color(0xFF575279),
)

private val RosePineDarkScheme = darkColorScheme(
    primary               = Color(0xFFEBBCBA),
    onPrimary             = Color(0xFF4A1020),
    primaryContainer      = Color(0xFF6D1E32),
    onPrimaryContainer    = Color(0xFFFFD9E2),
    secondary             = Color(0xFFC4A7E7),
    onSecondary           = Color(0xFF3B0063),
    secondaryContainer    = Color(0xFF541088),
    onSecondaryContainer  = Color(0xFFEFDBFF),
    tertiary              = Color(0xFF9CCFD8),
    onTertiary            = Color(0xFF003740),
    tertiaryContainer     = Color(0xFF005062),
    onTertiaryContainer   = Color(0xFFBFE8ED),
    background            = Color(0xFF191724),
    onBackground          = Color(0xFFE0DEF4),
    surface               = Color(0xFF1F1D2E),
    onSurface             = Color(0xFFE0DEF4),
    surfaceVariant        = Color(0xFF26233A),
    onSurfaceVariant      = Color(0xFFB2B0C7),
)

// ─── Ayu ──────────────────────────────────────────────────────────────────────
// Accent: #FFB454 dark / #FF8F40 light

private val AyuLightScheme = lightColorScheme(
    primary               = Color(0xFFFF8F40),
    onPrimary             = Color(0xFFFFFFFF),
    primaryContainer      = Color(0xFFFFDCC2),
    onPrimaryContainer    = Color(0xFF4A1800),
    secondary             = Color(0xFF36A3D9),
    onSecondary           = Color(0xFFFFFFFF),
    secondaryContainer    = Color(0xFFCCECFA),
    onSecondaryContainer  = Color(0xFF001F2C),
    tertiary              = Color(0xFF4CBF99),
    onTertiary            = Color(0xFFFFFFFF),
    tertiaryContainer     = Color(0xFFB3F0DC),
    onTertiaryContainer   = Color(0xFF00201A),
    background            = Color(0xFFFAFAFA),
    onBackground          = Color(0xFF1A1A1A),
)

private val AyuDarkScheme = darkColorScheme(
    primary               = Color(0xFFFFB454),
    onPrimary             = Color(0xFF4A2800),
    primaryContainer      = Color(0xFF6D3B00),
    onPrimaryContainer    = Color(0xFFFFDCC2),
    secondary             = Color(0xFF39BAE6),
    onSecondary           = Color(0xFF003546),
    secondaryContainer    = Color(0xFF004D65),
    onSecondaryContainer  = Color(0xFFB3E9FA),
    tertiary              = Color(0xFF7FD962),
    onTertiary            = Color(0xFF003A00),
    tertiaryContainer     = Color(0xFF005300),
    onTertiaryContainer   = Color(0xFF9AF67D),
    background            = Color(0xFF0D1017),
    onBackground          = Color(0xFFBFBDB6),
    surface               = Color(0xFF13191F),
    onSurface             = Color(0xFFBFBDB6),
    surfaceVariant        = Color(0xFF1A2030),
    onSurfaceVariant      = Color(0xFFA9B0C0),
)

// ─── Lookup ───────────────────────────────────────────────────────────────────

fun ColorTheme.toColorScheme(darkTheme: Boolean): ColorScheme = when (this) {
    ColorTheme.DEFAULT    -> if (darkTheme) darkColorScheme()  else lightColorScheme()
    ColorTheme.GITHUB     -> if (darkTheme) GitHubDarkScheme   else GitHubLightScheme
    ColorTheme.CATPPUCCIN -> if (darkTheme) CatppuccinDarkScheme else CatppuccinLightScheme
    ColorTheme.DRACULA    -> if (darkTheme) DraculaDarkScheme  else DraculaLightScheme
    ColorTheme.NORD       -> if (darkTheme) NordDarkScheme     else NordLightScheme
    ColorTheme.ROSE_PINE  -> if (darkTheme) RosePineDarkScheme else RosePineLightScheme
    ColorTheme.AYU        -> if (darkTheme) AyuDarkScheme      else AyuLightScheme
}

fun ColorTheme.displayName(): String = when (this) {
    ColorTheme.DEFAULT    -> "Default"
    ColorTheme.GITHUB     -> "GitHub"
    ColorTheme.CATPPUCCIN -> "Catppuccin"
    ColorTheme.DRACULA    -> "Dracula"
    ColorTheme.NORD       -> "Nord"
    ColorTheme.ROSE_PINE  -> "Rosé Pine"
    ColorTheme.AYU        -> "Ayu"
}
