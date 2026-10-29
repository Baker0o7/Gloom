package dev.materii.gloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
            typography = Typography(),
            shapes = Shapes(),
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
