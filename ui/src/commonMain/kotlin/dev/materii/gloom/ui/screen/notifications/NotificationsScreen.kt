package dev.materii.gloom.ui.screen.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CallMerge
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
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
import dev.materii.gloom.api.dto.notification.NotificationDto
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.screen.notifications.viewmodel.NotificationsViewModel
import dev.materii.gloom.util.TimeUtils.getTimeSince
import kotlinx.datetime.Instant

class NotificationsScreen : Tab {

    override val options: TabOptions
        @Composable get() {
            val navigator = LocalTabNavigator.current
            val selected = navigator.current == this
            return TabOptions(
                index = 3u,
                title = stringResource(Res.strings.navigation_inbox),
                icon = rememberVectorPainter(
                    if (selected) Icons.Filled.Notifications
                    else Icons.Outlined.Notifications
                )
            )
        }

    @Composable
    override fun Content() = Screen()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun Screen() {
        val viewModel: NotificationsViewModel = koinScreenModel()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val unreadCount = viewModel.notifications.count { it.unread }

        Scaffold(
            topBar = {
                LargeTopAppBar(
                    title = { Text(options.title) },
                    actions = {
                        if (unreadCount > 0) {
                            IconButton(onClick = { viewModel.markAllRead() }) {
                                Icon(
                                    imageVector = Icons.Outlined.DoneAll,
                                    contentDescription = "Mark all as read"
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { pv ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.load() },
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                when {
                    viewModel.isLoading && viewModel.notifications.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    viewModel.notifications.isEmpty() -> {
                        EmptyState()
                    }

                    else -> {
                        // Group by repo for cleaner visual hierarchy
                        val grouped = viewModel.notifications
                            .groupBy { it.repository.fullName }
                            .toList()

                        LazyColumn(Modifier.fillMaxSize()) {
                            grouped.forEach { (repoName, items) ->
                                stickyHeader(key = "header_$repoName") {
                                    RepoGroupHeader(
                                        repoName = repoName,
                                        avatarUrl = items.first().repository.owner.avatarUrl,
                                        unreadCount = items.count { it.unread }
                                    )
                                }
                                items(items, key = { it.id }) { notification ->
                                    NotificationItem(
                                        notification = notification,
                                        onClick = { viewModel.markRead(notification.id) }
                                    )
                                    ThinDivider()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Sticky repo header ───────────────────────────────────────────────────────

@Composable
private fun RepoGroupHeader(
    repoName: String,
    avatarUrl: String,
    unreadCount: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
        )
        Text(
            text = repoName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (unreadCount > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = unreadCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ─── Single notification row ──────────────────────────────────────────────────

@Composable
private fun NotificationItem(
    notification: NotificationDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (notification.unread)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                else
                    MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = notification.subject.type.toIcon(),
            contentDescription = null,
            tint = if (notification.unread)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = notification.subject.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (notification.unread) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reason chip
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = notification.reason.toLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                // Time
                val timeText = remember(notification.updatedAt) {
                    runCatching { getTimeSince(Instant.parse(notification.updatedAt)) }.getOrNull()
                }
                if (timeText != null) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Unread dot
        if (notification.unread) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "You're all caught up!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Mentions, reviews, and other activity\nwill show up here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun String.toIcon(): ImageVector = when (this) {
    "Issue"       -> Icons.Outlined.BugReport
    "PullRequest" -> Icons.Outlined.CallMerge
    "Release"     -> Icons.Outlined.NewReleases
    "Commit"      -> Icons.Outlined.Code
    "Discussion"  -> Icons.Outlined.Forum
    else          -> Icons.Outlined.Notifications
}

private fun String.toLabel(): String = when (this) {
    "assign"           -> "Assigned"
    "author"           -> "Author"
    "comment"          -> "Comment"
    "ci_activity"      -> "CI"
    "invitation"       -> "Invited"
    "manual"           -> "Subscribed"
    "mention"          -> "Mentioned"
    "review_requested" -> "Review requested"
    "security_alert"   -> "Security"
    "state_change"     -> "State changed"
    "subscribed"       -> "Watching"
    "team_mention"     -> "Team mentioned"
    else               -> this.replace("_", " ").replaceFirstChar { it.uppercase() }
}
