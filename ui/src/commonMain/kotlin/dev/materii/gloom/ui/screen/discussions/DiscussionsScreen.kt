package dev.materii.gloom.ui.screen.discussions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.materii.gloom.ui.component.LoadStateBox
import dev.materii.gloom.ui.component.ScreenScaffold
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.screen.discussions.viewmodel.DiscussionComment
import dev.materii.gloom.ui.screen.discussions.viewmodel.DiscussionSummary
import dev.materii.gloom.ui.screen.discussions.viewmodel.DiscussionViewModel
import dev.materii.gloom.ui.screen.discussions.viewmodel.DiscussionsViewModel
import dev.materii.gloom.ui.util.NavigationUtil.navigate
import dev.materii.gloom.util.TimeUtils
import kotlinx.datetime.Instant
import org.koin.core.parameter.parametersOf

private val AnsweredGreen = Color(0xFF3FB950)

private fun relative(iso: String?): String? =
    iso?.let { runCatching { TimeUtils.getTimeSince(Instant.parse(it)) }.getOrNull() }

class DiscussionsScreen(
    private val owner: String,
    private val name: String,
) : Screen {

    override val key: ScreenKey get() = "$owner/$name/discussions"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DiscussionsViewModel> {
            parametersOf(DiscussionsViewModel.Input(owner, name))
        }
        val nav = LocalNavigator.currentOrThrow

        ScreenScaffold(title = "Discussions", subtitle = "$owner/$name") { pv ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading && viewModel.discussions.isNotEmpty(),
                onRefresh = { viewModel.load() },
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                LoadStateBox(
                    isLoading = viewModel.isLoading,
                    hasError = viewModel.hasError,
                    isEmpty = viewModel.discussions.isEmpty(),
                    emptyText = if (viewModel.isEnabled) "No discussions yet"
                                else "Discussions are not enabled for this repository",
                    onRetry = { viewModel.load() }
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(viewModel.discussions, key = { it.number }) { discussion ->
                            DiscussionItem(
                                discussion,
                                onClick = { nav.navigate(DiscussionScreen(owner, name, discussion.number)) }
                            )
                            ThinDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscussionItem(discussion: DiscussionSummary, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "${discussion.emoji} ${discussion.category}".trim(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (discussion.answered) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Answered",
                    tint = AnsweredGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Text(
            discussion.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val time = remember(discussion.createdAt) { relative(discussion.createdAt) }
        Text(
            buildString {
                append("${discussion.author}")
                if (time != null) append(" · $time")
                append(" · ${discussion.comments} comments")
                if (discussion.upvotes > 0) append(" · ▲ ${discussion.upvotes}")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

class DiscussionScreen(
    private val owner: String,
    private val name: String,
    private val number: Int,
) : Screen {

    override val key: ScreenKey get() = "$owner/$name/discussions/$number"

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DiscussionViewModel> {
            parametersOf(DiscussionViewModel.Input(owner, name, number))
        }
        val detail = viewModel.detail

        ScreenScaffold(title = "Discussion #$number", subtitle = "$owner/$name") { pv ->
            LoadStateBox(
                isLoading = viewModel.isLoading,
                hasError = viewModel.hasError,
                isEmpty = detail == null,
                emptyText = "Discussion not found",
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(pv)
            ) {
                if (detail != null) {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                        item("head") {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(detail.title, style = MaterialTheme.typography.titleLarge)
                                val time = relative(detail.createdAt)
                                Text(
                                    buildString {
                                        append(detail.author)
                                        if (time != null) append(" · $time")
                                        if (detail.category.isNotEmpty()) append(" · ${detail.category}")
                                        if (detail.upvotes > 0) append(" · ▲ ${detail.upvotes}")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(detail.body, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        detail.answer?.let { answer ->
                            item("answer") {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Accepted answer",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = AnsweredGreen
                                )
                                Spacer(Modifier.height(4.dp))
                                CommentCard(answer)
                            }
                        }

                        item("comments_header") {
                            Spacer(Modifier.height(20.dp))
                            Text(
                                "${detail.comments.size} comments",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        items(detail.comments.size) { index ->
                            CommentCard(detail.comments[index])
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(comment: DiscussionComment) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val time = relative(comment.createdAt)
            Text(
                buildString {
                    append(comment.author)
                    if (time != null) append(" · $time")
                    if (comment.upvotes > 0) append(" · ▲ ${comment.upvotes}")
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(comment.body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
