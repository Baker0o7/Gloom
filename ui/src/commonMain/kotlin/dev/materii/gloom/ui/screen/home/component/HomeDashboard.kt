package dev.materii.gloom.ui.screen.home.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class QuickAction(
    val label: String,
    val icon: ImageVector,
    val badge: Int = 0,
    val onClick: () -> Unit,
)

/** Greeting plus a scrollable row of shortcuts, shown at the top of the home feed. */
@Composable
fun HomeDashboard(name: String?, actions: List<QuickAction>) {
    val greeting = remember {
        when (Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour) {
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(horizontal = 4.dp)) {
            Text(
                if (name.isNullOrBlank()) greeting else "$greeting, $name",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Jump to something, or catch up on your feed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            actions.forEach { action -> QuickActionTile(action) }
        }
    }
}

@Composable
private fun QuickActionTile(action: QuickAction) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.width(88.dp).clickable(onClick = action.onClick),
    ) {
        Column(
            Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            BadgedBox(badge = {
                if (action.badge > 0) Badge { Text(action.badge.coerceAtMost(99).toString()) }
            }) {
                Icon(action.icon, contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Text(
                action.label,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
