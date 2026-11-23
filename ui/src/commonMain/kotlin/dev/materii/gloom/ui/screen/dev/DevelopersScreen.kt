package dev.materii.gloom.ui.screen.dev

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.materii.gloom.ui.component.Avatar
import dev.materii.gloom.ui.component.LoadStateBox
import dev.materii.gloom.ui.component.ScreenScaffold
import dev.materii.gloom.ui.component.ThinDivider
import dev.materii.gloom.ui.screen.dev.viewmodel.DevelopersViewModel
import dev.materii.gloom.ui.screen.profile.ProfileScreen
import dev.materii.gloom.ui.util.NavigationUtil.navigate

class DevelopersScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<DevelopersViewModel>()
        val nav = LocalNavigator.currentOrThrow

        ScreenScaffold(title = "Explore developers") { pv ->
            Column(
                Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DevelopersViewModel.Filter.entries.forEach { option ->
                        FilterChip(
                            selected = viewModel.filter == option,
                            onClick = { viewModel.select(option) },
                            label = { Text(option.label) }
                        )
                    }
                }

                PullToRefreshBox(
                    isRefreshing = viewModel.isLoading && viewModel.developers.isNotEmpty(),
                    onRefresh = { viewModel.load() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LoadStateBox(
                        isLoading = viewModel.isLoading,
                        hasError = viewModel.hasError,
                        isEmpty = viewModel.developers.isEmpty(),
                        emptyText = "No developers found",
                        onRetry = { viewModel.load() }
                    ) {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(viewModel.developers, key = { it.login }) { developer ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { nav.navigate(ProfileScreen(developer.login)) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Avatar(url = developer.avatarUrl, modifier = Modifier.size(44.dp))
                                    Column {
                                        Text(developer.login, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            developer.type,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                ThinDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}
