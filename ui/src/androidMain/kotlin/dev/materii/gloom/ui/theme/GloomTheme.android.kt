package dev.materii.gloom.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import dev.materii.gloom.domain.manager.enums.ColorTheme
import dev.materii.gloom.util.supportsMonet

@Composable
actual fun getColorSchemes(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    colorTheme: ColorTheme
): Pair<ColorScheme, GloomColorScheme> {
    // Dynamic color (Monet) overrides the selected palette on Android 12+
    val colorScheme = when {
        dynamicColor && darkTheme && supportsMonet  -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme && supportsMonet -> dynamicLightColorScheme(LocalContext.current)
        else                                        -> colorTheme.toColorScheme(darkTheme)
    }
    val gloomColors = if (darkTheme) darkGloomColorScheme() else lightGloomColorScheme()
    return colorScheme to gloomColors
}