package dev.materii.gloom.ui.screen.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import dev.materii.gloom.gql.SearchQuery
import dev.materii.gloom.ui.component.Avatar
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.profile.ProfileScreen
import dev.materii.gloom.ui.screen.repo.RepoScreen
import dev.materii.gloom.ui.screen.search.viewmodel.SearchViewModel
import dev.materii.gloom.ui.util.NavigationUtil.navigate
import dev.materii.gloom.util.NumberFormatter

class SearchScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel      = koinScreenModel<SearchViewModel>()
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
        val listState      = rememberLazyListState()

        // Load more when near bottom
        val nearBottom by remember {
            derivedStateOf {
                val last    = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total   = listState.layoutInfo.totalItemsCount
                total > 0 && last >= total - 3
            }
        }
        LaunchedEffect(nearBottom) { if (nearBottom) viewModel.loadMore() }

        Scaffold(
            topBar = {
                LargeToolbar(title = "Search", scrollBehavior = scrollBehavior)
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { pv ->
            Column(Modifier.padding(pv).fillMaxSize()) {

                // Search field
                OutlinedTextField(
                    value         = viewModel.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder   = { Text("Search GitHub…") },
                    leadingIcon   = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon  = {
                        if (viewModel.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Outlined.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(24.dp),
                )

                // Tabs
                TabRow(selectedTabIndex = viewModel.activeTab.ordinal) {
                    Tab(
                        selected = viewModel.activeTab == SearchViewModel.Tab.REPOSITORIES,
                        onClick  = { viewModel.onTabChange(SearchViewModel.Tab.REPOSITORIES) },
                        text     = { Text("Repositories") }
                    )
                    Tab(
                        selected = viewModel.activeTab == SearchViewModel.Tab.USERS,
                        onClick  = { viewModel.onTabChange(SearchViewModel.Tab.USERS) },
                        text     = { Text("Users") }
                    )
                }

                when {
                    viewModel.isLoading && viewModel.results.isEmpty() ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }

                    viewModel.query.isBlank() ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Search for repos, users, or orgs",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                    viewModel.results.isEmpty() && !viewModel.isLoading ->
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("No results for "${viewModel.query}"",
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                    else -> {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            itemsIndexed(viewModel.results) { _, node ->
                                SearchResultRow(node)
                                ThinDivider()
                            }
                            if (viewModel.isLoading) {
                                item {
                                    Box(Modifier.fillMaxWidth().padding(16.dp), Alignment.Center) {
                                        CircularProgressIndicator(Modifier.size(24.dp))
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

@Composable
private fun SearchResultRow(node: SearchQuery.Node) {
    val nav = LocalNavigator.currentOrThrow
    when (node.__typename) {
        "Repository" -> {
            val r = node.onRepository ?: return
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(RepoScreen(r.owner.login, r.name)) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                AsyncImage(
                    model = r.owner.avatarUrl, contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${r.owner.login}/${r.name}",
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    r.description?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(
                    "★ ${NumberFormatter.compact(r.stargazerCount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        "User", "Organization" -> {
            val login  = node.onUser?.login      ?: node.onOrganization?.login      ?: return
            val name   = node.onUser?.name       ?: node.onOrganization?.name
            val avatar = node.onUser?.avatarUrl  ?: node.onOrganization?.avatarUrl  ?: return
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.navigate(ProfileScreen(login)) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Avatar(url = avatar, contentDescription = login,
                    modifier = Modifier.size(36.dp))
                Column(Modifier.weight(1f)) {
                    if (name != null) {
                        Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(login, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text(login, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
