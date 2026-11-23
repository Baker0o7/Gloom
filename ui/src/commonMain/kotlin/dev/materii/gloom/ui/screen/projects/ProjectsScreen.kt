package dev.materii.gloom.ui.screen.projects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import dev.materii.gloom.ui.screen.projects.viewmodel.ProjectBoardViewModel
import dev.materii.gloom.ui.screen.projects.viewmodel.ProjectItem
import dev.materii.gloom.ui.screen.projects.viewmodel.ProjectSummary
import dev.materii.gloom.ui.screen.projects.viewmodel.ProjectsViewModel
import dev.materii.gloom.ui.util.NavigationUtil.navigate
import org.koin.core.parameter.parametersOf

class ProjectsScreen(
    private val owner: String,
    private val name: String,
) : Screen {

    override val key: ScreenKey get() = "$owner/$name/projects"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ProjectsViewModel> {
            parametersOf(ProjectsViewModel.Input(owner, name))
        }
        val nav = LocalNavigator.currentOrThrow

        ScreenScaffold(title = "Projects", subtitle = "$owner/$name") { pv ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading && viewModel.projects.isNotEmpty(),
                onRefresh = { viewModel.load() },
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                LoadStateBox(
                    isLoading = viewModel.isLoading,
                    hasError = viewModel.errorMessage != null,
                    isEmpty = viewModel.projects.isEmpty(),
                    emptyText = "No projects are linked to this repository",
                    errorText = viewModel.errorMessage ?: "Couldn't load projects.",
                    onRetry = { viewModel.load() }
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(viewModel.projects, key = { it.id }) { project ->
                            ProjectItemRow(
                                project,
                                onClick = { nav.navigate(ProjectBoardScreen(project.id, project.title)) }
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
private fun ProjectItemRow(project: ProjectSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(project.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                buildString {
                    append("#${project.number} · ${project.itemCount} items")
                    if (project.closed) append(" · closed")
                    if (project.description.isNotBlank()) append("\n${project.description}")
                },
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

class ProjectBoardScreen(
    private val projectId: String,
    private val title: String,
) : Screen {

    override val key: ScreenKey get() = "project/$projectId"

    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ProjectBoardViewModel> {
            parametersOf(ProjectBoardViewModel.Input(projectId))
        }

        ScreenScaffold(title = title) { pv ->
            LoadStateBox(
                isLoading = viewModel.isLoading,
                hasError = viewModel.errorMessage != null,
                isEmpty = viewModel.columns.isEmpty(),
                emptyText = "This project has no items yet",
                errorText = viewModel.errorMessage ?: "Couldn't load this project.",
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(pv)
            ) {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    viewModel.columns.forEach { column ->
                        item("header_${column.name}") {
                            Text(
                                "${column.name} · ${column.items.size}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp)
                            )
                        }
                        items(column.items.size) { index -> BoardItem(column.items[index]) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardItem(item: ProjectItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.title, style = MaterialTheme.typography.bodyMedium)
            val kind = when (item.type) {
                "Issue" -> "Issue"
                "PullRequest" -> "Pull request"
                else -> "Draft"
            }
            Text(
                buildString {
                    append(kind)
                    if (item.number != null) append(" #${item.number}")
                    item.state?.let { append(" · ${it.lowercase()}") }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
