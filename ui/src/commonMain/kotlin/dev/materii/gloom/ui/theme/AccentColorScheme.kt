package dev.materii.gloom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.materii.gloom.domain.manager.enums.Accent

/**
 * Hand-tuned Material 3 palettes used when dynamic color is off or unsupported,
 * so the app isn't stuck with the stock purple scheme.
 */
fun accentColorScheme(accent: Accent, dark: Boolean, amoled: Boolean): ColorScheme {
    val scheme = if (dark) darkAccent(accent) else lightAccent(accent)
    return if (dark && amoled) scheme.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF121212),
        surfaceTint = Color.Transparent
    ) else scheme
}

private fun lightAccent(accent: Accent): ColorScheme = when (accent) {
    Accent.BLUE -> lightColorScheme(
        primary = Color(0xFF0B57D0), onPrimary = Color.White,
        primaryContainer = Color(0xFFD3E3FD), onPrimaryContainer = Color(0xFF041E49),
        secondaryContainer = Color(0xFFDDE3EA), onSecondaryContainer = Color(0xFF151C23)
    )
    Accent.GREEN -> lightColorScheme(
        primary = Color(0xFF2E6B34), onPrimary = Color.White,
        primaryContainer = Color(0xFFB0F2AF), onPrimaryContainer = Color(0xFF00210A),
        secondaryContainer = Color(0xFFD3E8D0), onSecondaryContainer = Color(0xFF0E1F11)
    )
    Accent.TEAL -> lightColorScheme(
        primary = Color(0xFF00695C), onPrimary = Color.White,
        primaryContainer = Color(0xFFA0F2E1), onPrimaryContainer = Color(0xFF00201B),
        secondaryContainer = Color(0xFFCCE8E2), onSecondaryContainer = Color(0xFF05201C)
    )
    Accent.ORANGE -> lightColorScheme(
        primary = Color(0xFF8B5000), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDCBE), onPrimaryContainer = Color(0xFF2C1600),
        secondaryContainer = Color(0xFFF0DFD0), onSecondaryContainer = Color(0xFF251A0E)
    )
    Accent.PINK -> lightColorScheme(
        primary = Color(0xFF9C2A5E), onPrimary = Color.White,
        primaryContainer = Color(0xFFFFD9E3), onPrimaryContainer = Color(0xFF3E0021),
        secondaryContainer = Color(0xFFF4DDE3), onSecondaryContainer = Color(0xFF2A151B)
    )
    Accent.DEFAULT -> lightColorScheme()
}

private fun darkAccent(accent: Accent): ColorScheme = when (accent) {
    Accent.BLUE -> darkColorScheme(
        primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF062E6F),
        primaryContainer = Color(0xFF0842A0), onPrimaryContainer = Color(0xFFD3E3FD),
        secondaryContainer = Color(0xFF3B4655), onSecondaryContainer = Color(0xFFDDE3EA)
    )
    Accent.GREEN -> darkColorScheme(
        primary = Color(0xFF95D694), onPrimary = Color(0xFF00390F),
        primaryContainer = Color(0xFF145220), onPrimaryContainer = Color(0xFFB0F2AF),
        secondaryContainer = Color(0xFF384B3B), onSecondaryContainer = Color(0xFFD3E8D0)
    )
    Accent.TEAL -> darkColorScheme(
        primary = Color(0xFF83D5C6), onPrimary = Color(0xFF00382F),
        primaryContainer = Color(0xFF005046), onPrimaryContainer = Color(0xFFA0F2E1),
        secondaryContainer = Color(0xFF334B47), onSecondaryContainer = Color(0xFFCCE8E2)
    )
    Accent.ORANGE -> darkColorScheme(
        primary = Color(0xFFFFB870), onPrimary = Color(0xFF4A2800),
        primaryContainer = Color(0xFF6A3C00), onPrimaryContainer = Color(0xFFFFDCBE),
        secondaryContainer = Color(0xFF564334), onSecondaryContainer = Color(0xFFF0DFD0)
    )
    Accent.PINK -> darkColorScheme(
        primary = Color(0xFFFFB0CB), onPrimary = Color(0xFF5E1139),
        primaryContainer = Color(0xFF7C2951), onPrimaryContainer = Color(0xFFFFD9E3),
        secondaryContainer = Color(0xFF5B3F47), onSecondaryContainer = Color(0xFFF4DDE3)
    )
    Accent.DEFAULT -> darkColorScheme()
}
