package dev.materii.gloom.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.gql.FeedQuery
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.home.component.CreatedRepoItem
import dev.materii.gloom.ui.screen.home.component.FollowedUserItem
import dev.materii.gloom.ui.screen.home.component.ForkedRepoItem
import dev.materii.gloom.ui.screen.home.component.MergedPullRequestItem
import dev.materii.gloom.ui.screen.home.component.NewReleaseItem
import dev.materii.gloom.ui.screen.home.component.RecommendedFollowUserItem
import dev.materii.gloom.ui.screen.home.component.RecommendedRepoItem
import dev.materii.gloom.ui.screen.home.component.StarredRepoItem
import dev.materii.gloom.ui.screen.home.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

class HomeScreen : Tab {

    override val options: TabOptions
        @Composable get() {
            val navigator = LocalTabNavigator.current
            val selected  = navigator.current == this
            return TabOptions(
                0u,
                stringResource(Res.strings.navigation_home),
                rememberVectorPainter(if (selected) Icons.Filled.Home else Icons.Outlined.Home)
            )
        }

    @Composable
    @OptIn(ExperimentalMaterial3Api::class)
    override fun Content() {
        val viewModel      = koinScreenModel<HomeViewModel>()
        val items          = viewModel.items.collectAsLazyPagingItems()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val listState      = rememberLazyListState()
        val scope          = rememberCoroutineScope()

        val isRefreshing = items.loadState.refresh == LoadState.Loading
        val hasError     = items.loadState.refresh is LoadState.Error
        val isEmpty      = !isRefreshing && items.itemCount == 0 && !hasError
        val canScrollUp  by remember { derivedStateOf { listState.canScrollBackward } }

        Scaffold(
            topBar = {
                LargeToolbar(options.title, scrollBehavior = scrollBehavior)
            },
            floatingActionButton = {
                AnimatedVisibility(canScrollUp, enter = scaleIn(), exit = scaleOut()) {
                    SmallFloatingActionButton(onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                            .invokeOnCompletion { viewModel.refresh(items) }
                    }) {
                        Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Scroll to top")
                    }
                }
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { pv ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh    = { viewModel.refresh(items) },
                modifier     = Modifier.fillMaxSize().padding(pv),
            ) {
                when {
                    // Full-page error state
                    hasError && items.itemCount == 0 -> FeedErrorState(
                        onRetry = { items.retry() }
                    )
                    // Empty feed
                    isEmpty -> FeedEmptyState()
                    // Normal list
                    else -> LazyColumn(
                        state    = listState,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(
                            count       = items.itemCount,
                            key         = items.itemKey(),
                            contentType = items.itemContentType(),
                        ) { index ->
                            items[index]?.let { FeedItem(it, viewModel) }
                            ThinDivider()
                        }

                        // Append error footer
                        if (items.loadState.append is LoadState.Error) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    Alignment.Center
                                ) {
                                    OutlinedButton(onClick = { items.retry() }) {
                                        Text("Load more failed — tap to retry")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Feed item dispatcher ─────────────────────────────────────────────────────

@Composable
private fun FeedItem(feedItem: FeedQuery.Node, viewModel: HomeViewModel) {
    when {
        feedItem.createdRepoItemFragment != null -> {
            val repo = feedItem.createdRepoItemFragment!!
            CreatedRepoItem(
                item          = repo,
                starData      = viewModel.starredRepos[repo.repository.feedRepository.id],
                onStarClick   = viewModel::starRepo,
                onUnstarClick = viewModel::unstarRepo,
            )
        }

        feedItem.newReleaseItemFragment != null ->
            NewReleaseItem(item = feedItem.newReleaseItemFragment!!)

        feedItem.followedUserFeedItemFragment != null -> {
            val item = feedItem.followedUserFeedItemFragment!!
            val id   = item.followee.feedUser?.id ?: item.followee.feedOrg?.id!!
            FollowedUserItem(
                item            = item,
                followData      = viewModel.followedUsers[id],
                onFollowClick   = viewModel::followUser,
                onUnfollowClick = viewModel::unfollowUser,
            )
        }

        feedItem.starredFeedItemFragment != null -> {
            val repo = feedItem.starredFeedItemFragment!!
            StarredRepoItem(
                item          = repo,
                starData      = viewModel.starredRepos[repo.repository.feedRepository.id],
                onStarClick   = viewModel::starRepo,
                onUnstarClick = viewModel::unstarRepo,
            )
        }

        feedItem.recommendedRepositoryFeedItemFragment != null -> {
            val repo = feedItem.recommendedRepositoryFeedItemFragment!!
            RecommendedRepoItem(
                item          = repo,
                starData      = viewModel.starredRepos[repo.repository.feedRepository.id],
                onStarClick   = viewModel::starRepo,
                onUnstarClick = viewModel::unstarRepo,
            )
        }

        feedItem.forkedRepositoryFeedItemFragment != null -> {
            val repo = feedItem.forkedRepositoryFeedItemFragment!!
            ForkedRepoItem(
                item          = repo,
                starData      = viewModel.starredRepos[repo.repository.feedRepository.id],
                onStarClick   = viewModel::starRepo,
                onUnstarClick = viewModel::unstarRepo,
            )
        }

        feedItem.followRecommendationFeedItemFragment != null -> {
            val item = feedItem.followRecommendationFeedItemFragment!!
            val id   = item.followee.feedUser?.id ?: item.followee.feedOrg?.id!!
            RecommendedFollowUserItem(
                item            = item,
                followData      = viewModel.followedUsers[id],
                onFollowClick   = viewModel::followUser,
                onUnfollowClick = viewModel::unfollowUser,
            )
        }

        feedItem.mergedPullRequestFeedItemFragment != null -> {
            val pr = feedItem.mergedPullRequestFeedItemFragment!!
            MergedPullRequestItem(
                item            = pr,
                onReactionClick = { reaction, unreact ->
                    viewModel.react(pr.pullRequest.id, reaction, unreact)
                }
            )
        }
    }
}

// ─── Empty / error states ─────────────────────────────────────────────────────

@Composable
private fun FeedEmptyState() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Text(
            "Your feed is empty.\nFollow people to see their activity here.",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(32.dp),
        )
    }
}

@Composable
private fun FeedErrorState(onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp),
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Text(
                "Couldn't load your feed.\nCheck your connection and try again.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) { Text("Retry") }
        }
    }
}
