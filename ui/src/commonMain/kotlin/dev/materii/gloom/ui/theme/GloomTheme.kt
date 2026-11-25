package dev.materii.gloom.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.materii.gloom.domain.manager.enums.Accent
import dev.materii.gloom.util.supportsMonet

@Composable
fun GloomTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    amoled: Boolean = false,
    accent: Accent = Accent.DEFAULT,
    content: @Composable () -> Unit
) {
    val (baseColors, gloomColors) = getColorSchemes(darkTheme, dynamicColor, amoled)
    val useDynamic = dynamicColor && supportsMonet
    val colors = if (accent != Accent.DEFAULT && !useDynamic)
        accentColorScheme(accent, darkTheme, amoled)
    else baseColors

    CompositionLocalProvider(
        LocalGloomColorScheme provides gloomColors
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = gloomTypography(),
            shapes = gloomShapes,
            content = content
        )
    }
}

/**
 * Retrieves the color schemes to be used based on user settings
 *
 * @param darkTheme Whether or not to use the dark theme variant
 * @param dynamicColor (Android 12+ only) Whether or not to use a dynamic color scheme
 * @param amoled Whether or not to force the AMOLED color scheme
 */
@Composable
expect fun getColorSchemes(darkTheme: Boolean, dynamicColor: Boolean, amoled: Boolean): Pair<ColorScheme, GloomColorScheme>

/** Softer, rounder corners than the Material defaults: cards, dialogs, sheets and chips all follow. */
private val gloomShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

/** Material typography with heavier titles so headings read clearly against body text. */
private fun gloomTypography(): Typography {
    val base = Typography()
    return base.copy(
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium),
    )
}
