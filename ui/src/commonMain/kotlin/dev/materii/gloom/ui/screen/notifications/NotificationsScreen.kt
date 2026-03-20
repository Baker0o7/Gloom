package dev.materii.gloom.ui.screen.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import coil3.compose.AsyncImage
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.api.dto.notification.Notification
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.icon.Custom
import dev.materii.gloom.ui.icon.custom.Commit
import dev.materii.gloom.ui.icon.custom.OpenPullRequest
import dev.materii.gloom.ui.screen.notifications.viewmodel.NotificationsViewModel

class NotificationsScreen : Tab {

    override val options: TabOptions
        @Composable get() {
            val navigator = LocalTabNavigator.current
            val selected  = navigator.current == this
            return TabOptions(
                0u,
                stringResource(Res.strings.navigation_inbox),
                rememberVectorPainter(
                    if (selected) Icons.Filled.Notifications else Icons.Outlined.Notifications
                )
            )
        }

    @Composable
    override fun Content() = Screen()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen() {
        val viewModel      = koinScreenModel<NotificationsViewModel>()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val unreadCount    = viewModel.notifications.count { it.unread }

        Scaffold(
            topBar = {
                LargeToolbar(
                    title          = stringResource(Res.strings.navigation_inbox),
                    scrollBehavior = scrollBehavior,
                    actions        = {
                        if (unreadCount > 0) {
                            IconButton(onClick = viewModel::markAllRead) {
                                Icon(Icons.Outlined.DoneAll, contentDescription = "Mark all read")
                            }
                        }
                        IconButton(onClick = viewModel::load) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { pv ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh    = viewModel::load,
                modifier     = Modifier.fillMaxSize().padding(pv),
            ) {
                when {
                    viewModel.isLoading && viewModel.notifications.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    viewModel.error != null && viewModel.notifications.isEmpty() -> {
                        ErrorState(onRetry = viewModel::load)
                    }

                    viewModel.notifications.isEmpty() -> {
                        EmptyState()
                    }

                    else -> {
                        NotificationList(
                            notifications = viewModel.notifications,
                            onMarkRead    = viewModel::markRead,
                        )
                    }
                }
            }
        }
    }
}

// ─── Notification list ────────────────────────────────────────────────────────

@Composable
private fun NotificationList(
    notifications: List<Notification>,
    onMarkRead: (String) -> Unit,
) {
    // Group by repository
    val grouped = notifications.groupBy { it.repository.fullName }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        grouped.forEach { (repoFullName, items) ->
            // Sticky-ish repo header
            item(key = "header_$repoFullName") {
                RepoHeader(
                    repoFullName = repoFullName,
                    avatarUrl    = items.first().repository.owner.avatarUrl,
                    unreadCount  = items.count { it.unread },
                )
            }
            items(items, key = { it.id }) { notification ->
                NotificationRow(
                    notification = notification,
                    onMarkRead   = { onMarkRead(notification.id) },
                )
                ThinDivider()
            }
        }
    }
}

// ─── Repo header ──────────────────────────────────────────────────────────────

@Composable
private fun RepoHeader(repoFullName: String, avatarUrl: String, unreadCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AsyncImage(
            model              = avatarUrl,
            contentDescription = null,
            modifier           = Modifier.size(20.dp).clip(CircleShape),
        )
        Text(
            text      = repoFullName,
            style     = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier  = Modifier.weight(1f),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
        )
        if (unreadCount > 0) {
            Box(
                modifier          = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment  = Alignment.Center,
            ) {
                Text(
                    text  = unreadCount.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

// ─── Single notification row ──────────────────────────────────────────────────

@Composable
private fun NotificationRow(notification: Notification, onMarkRead: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (notification.unread) MaterialTheme.colorScheme.surfaceContainerLow
                else MaterialTheme.colorScheme.surface
            )
            .clickable(enabled = notification.unread) { onMarkRead() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Unread dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (notification.unread) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface
                )
        )

        // Subject type icon
        Icon(
            imageVector        = notification.subject.type.toIcon(),
            contentDescription = notification.subject.type,
            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier           = Modifier.size(18.dp),
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text      = notification.subject.title,
                style     = MaterialTheme.typography.bodyMedium,
                fontWeight = if (notification.unread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines  = 2,
                overflow  = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                ReasonChip(notification.reason)
                Text(
                    text  = notification.subject.type,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (notification.unread) {
            IconButton(onClick = onMarkRead, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "Mark as read",
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun ReasonChip(reason: String) {
    val label = when (reason) {
        "assign"             -> "Assigned"
        "author"             -> "Author"
        "comment"            -> "Comment"
        "invitation"         -> "Invited"
        "manual"             -> "Subscribed"
        "mention"            -> "Mentioned"
        "review_requested"   -> "Review requested"
        "security_alert"     -> "Security alert"
        "state_change"       -> "State changed"
        "subscribed"         -> "Watching"
        "team_mention"       -> "Team mentioned"
        "ci_activity"        -> "CI activity"
        else                 -> reason.replace('_', ' ')
    }
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

// ─── Subject type → icon ──────────────────────────────────────────────────────

private fun String.toIcon(): ImageVector = when (this) {
    "PullRequest" -> Icons.Custom.OpenPullRequest
    "Release"     -> Icons.Outlined.LocalOffer
    "Commit"      -> Icons.Custom.Commit
    else          -> Icons.Outlined.Notifications   // Issue + default
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Outlined.Notifications,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("All caught up!", style = MaterialTheme.typography.titleMedium)
            Text(
                "No notifications right now.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Error state ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint     = MaterialTheme.colorScheme.error,
            )
            Text("Couldn't load notifications", style = MaterialTheme.typography.titleMedium)
            Text(
                "Check your connection and try again.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
