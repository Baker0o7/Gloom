package dev.materii.gloom.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.materii.gloom.util.supportsMonet

@Composable
actual fun getColorSchemes(darkTheme: Boolean, dynamicColor: Boolean, amoled: Boolean): Pair<ColorScheme, GloomColorScheme> {
    // We don't technically need to check for dynamic theming support
    // here because its locked behind a setting that itself is SDK restricted
    // but its good to be cautious anyways.
    return when {
        amoled && dynamicColor && supportsMonet    -> dynamicDarkColorScheme(LocalContext.current).copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF121212),
            surfaceTint = Color.Transparent
        ) to darkGloomColorScheme()
        amoled                                     -> amoledColorScheme() to darkGloomColorScheme()
        dynamicColor && darkTheme && supportsMonet  -> dynamicDarkColorScheme(LocalContext.current) to darkGloomColorScheme()
        dynamicColor && !darkTheme && supportsMonet -> dynamicLightColorScheme(LocalContext.current) to lightGloomColorScheme()
        darkTheme                                   -> darkColorScheme() to darkGloomColorScheme()
        else                                        -> lightColorScheme() to lightGloomColorScheme()
    }
}
