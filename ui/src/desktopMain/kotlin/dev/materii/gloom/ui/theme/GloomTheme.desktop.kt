package dev.materii.gloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import dev.materii.gloom.domain.manager.enums.ColorTheme

@Composable
actual fun getColorSchemes(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    colorTheme: ColorTheme
): Pair<ColorScheme, GloomColorScheme> {
    return colorTheme.toColorScheme(darkTheme) to
        if (darkTheme) darkGloomColorScheme() else lightGloomColorScheme()
}