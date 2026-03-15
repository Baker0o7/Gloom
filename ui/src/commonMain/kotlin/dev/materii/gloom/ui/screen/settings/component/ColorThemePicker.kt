package dev.materii.gloom.ui.screen.settings.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.materii.gloom.domain.manager.enums.ColorTheme
import dev.materii.gloom.ui.theme.displayName
import dev.materii.gloom.ui.theme.toColorScheme

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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Color Theme",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
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
    val scheme = theme.toColorScheme(darkTheme)
    val primaryColor = scheme.primary
    val containerColor = scheme.primaryContainer

    val animatedBorderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "border"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
            }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .border(
                    width = if (selected) 3.dp else 1.5.dp,
                    color = animatedBorderColor,
                    shape = CircleShape
                )
                // Draw split circle: left half = primaryContainer, right half = primary
                .drawBehind {
                    val halfWidth = size.width / 2f
                    // Left half
                    drawRect(
                        color = containerColor,
                        topLeft = Offset.Zero,
                        size = Size(halfWidth, size.height)
                    )
                    // Right half
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(halfWidth, 0f),
                        size = Size(halfWidth, size.height)
                    )
                }
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = if (primaryColor.luminance() > 0.4f) Color.Black else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Text(
            text = theme.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

private fun Color.luminance(): Float =
    0.299f * red + 0.587f * green + 0.114f * blue
