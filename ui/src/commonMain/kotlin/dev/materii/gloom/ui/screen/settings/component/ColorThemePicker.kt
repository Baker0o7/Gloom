package dev.materii.gloom.ui.screen.settings.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.materii.gloom.domain.manager.enums.ColorTheme
import dev.materii.gloom.ui.theme.displayName
import dev.materii.gloom.ui.theme.toColorScheme

// Representative swatch color (primary) for each theme in both modes
private fun ColorTheme.swatchColor(darkTheme: Boolean): Color =
    toColorScheme(darkTheme).primary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorThemePicker(
    current: ColorTheme,
    darkTheme: Boolean,
    onSelect: (ColorTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Color Theme",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ColorTheme.entries.forEach { theme ->
                ThemeSwatch(
                    theme = theme,
                    darkTheme = darkTheme,
                    selected = theme == current,
                    onClick = { onSelect(theme) }
                )
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: ColorTheme,
    darkTheme: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val swatchColor = theme.swatchColor(darkTheme)
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                    width = if (selected) 3.dp else 1.5.dp,
                    color = borderColor,
                    shape = CircleShape
                )
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = if (swatchColor.luminance() > 0.4f) Color.Black else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Text(
            text = theme.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// Simple luminance approximation to determine icon color on swatch
private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
