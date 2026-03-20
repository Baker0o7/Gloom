package dev.materii.gloom.ui.screen.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.api.model.ModelRepo
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.component.toolbar.LargeToolbar
import dev.materii.gloom.ui.screen.list.viewmodel.StarredReposListViewModel
import dev.materii.gloom.ui.screen.repo.component.RepoItem
import org.koin.core.parameter.parametersOf

class StarredReposListScreen(
    private val username: String,
) : Screen {

    override val key: ScreenKey
        get() = "${this::class.simpleName}($username)"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel: StarredReposListViewModel = koinScreenModel { parametersOf(username) }
        val items          = viewModel.items.collectAsLazyPagingItems()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val isLoading      = items.loadState.refresh == LoadState.Loading
        val listState      = rememberLazyListState()

        var query by rememberSaveable { mutableStateOf("") }

        Scaffold(
            topBar = {
                LargeToolbar(
                    title          = stringResource(Res.strings.title_starred),
                    scrollBehavior = scrollBehavior,
                )
            },
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        ) { pv ->
            Column(
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                OutlinedTextField(
                    value         = query,
                    onValueChange = { query = it },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder   = { Text("Search starred repos…") },
                    leadingIcon   = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon  = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape      = RoundedCornerShape(24.dp),
                )

                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh    = { items.refresh() },
                    modifier     = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        items(
                            count       = items.itemCount,
                            key         = items.itemKey(),
                            contentType = items.itemContentType(),
                        ) { index ->
                            val item = items[index] ?: return@items
                            if (query.isBlank() || item.matchesQuery(query)) {
                                RepoItem(repo = item)
                                ThinDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun ModelRepo.matchesQuery(query: String): Boolean {
    val q = query.trim().lowercase()
    return name?.lowercase()?.contains(q) == true
        || description?.lowercase()?.contains(q) == true
        || owner?.username?.lowercase()?.contains(q) == true
        || language?.name?.lowercase()?.contains(q) == true
}
