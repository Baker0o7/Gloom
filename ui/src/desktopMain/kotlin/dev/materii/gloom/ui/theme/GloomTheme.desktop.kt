package dev.materii.gloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun getColorSchemes(darkTheme: Boolean, dynamicColor: Boolean, amoled: Boolean): Pair<ColorScheme, GloomColorScheme> {
    return when {
        amoled    -> amoledColorScheme() to darkGloomColorScheme()
        darkTheme -> darkColorScheme() to darkGloomColorScheme()
        else      -> lightColorScheme() to lightGloomColorScheme()
    }
}
