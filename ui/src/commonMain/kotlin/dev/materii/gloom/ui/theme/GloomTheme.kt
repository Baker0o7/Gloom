package dev.materii.gloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.materii.gloom.domain.manager.enums.ColorTheme

@Composable
fun GloomTheme(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    colorTheme: ColorTheme = ColorTheme.DEFAULT,
    content: @Composable () -> Unit
) {
    val (colors, gloomColors) = getColorSchemes(darkTheme, dynamicColor, colorTheme)

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
 * @param colorTheme The selected color palette theme
 */
@Composable
expect fun getColorSchemes(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    colorTheme: ColorTheme = ColorTheme.DEFAULT
): Pair<ColorScheme, GloomColorScheme>