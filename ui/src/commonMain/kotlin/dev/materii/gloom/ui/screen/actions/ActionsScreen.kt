package dev.materii.gloom.ui.screen.actions

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import dev.materii.gloom.api.dto.actions.WorkflowJob
import dev.materii.gloom.api.dto.actions.WorkflowRun
import dev.materii.gloom.ui.component.BackButton
import dev.materii.gloom.ui.component.ErrorMessage
import dev.materii.gloom.ui.screen.actions.viewmodel.ActionsViewModel
import org.koin.core.parameter.parametersOf

private val SuccessGreen = Color(0xFF3FB950)

class ActionsScreen(
    private val owner: String,
    private val name: String,
) : Screen {

    override val key: ScreenKey get() = "$owner/$name/actions"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<ActionsViewModel> { parametersOf(ActionsViewModel.Input(owner, name)) }
        val snackbar = remember { SnackbarHostState() }

        LaunchedEffect(viewModel.message) {
            viewModel.message?.let {
                snackbar.showSnackbar(it)
                viewModel.message = null
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("Actions")
                            Text(
                                "$owner/$name",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = { BackButton() },
                    actions = {
                        IconButton(onClick = { viewModel.load() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbar) }
        ) { pv ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading,
                onRefresh = { viewModel.load() },
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                LazyColumn(Modifier.fillMaxSize()) {
                    if (viewModel.runs.isEmpty() && !viewModel.isLoading) {
                        item("empty") {
                            if (viewModel.hasError) {
                                ErrorMessage(
                                    message = "Couldn't load workflow runs",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 64.dp),
                                    onRetryClick = { viewModel.load() }
                                )
                            } else {
                                Text(
                                    "No workflow runs yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 64.dp, start = 16.dp, end = 16.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    items(viewModel.runs, key = { it.id }) { run ->
                        RunItem(
                            run = run,
                            expanded = viewModel.expandedRunId == run.id,
                            jobs = viewModel.jobs[run.id],
                            onToggle = { viewModel.toggleExpanded(run) },
                            onCancel = { viewModel.cancel(run) },
                            onRerun = { viewModel.rerun(run) },
                            onRerunFailed = { viewModel.rerunFailed(run) },
                            onJobClick = { viewModel.showLogs(it) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        viewModel.logState?.let { LogDialog(it, onDismiss = viewModel::dismissLogs) }
    }
}

@Composable
private fun RunItem(
    run: WorkflowRun,
    expanded: Boolean,
    jobs: List<WorkflowJob>?,
    onToggle: () -> Unit,
    onCancel: () -> Unit,
    onRerun: () -> Unit,
    onRerunFailed: () -> Unit,
    onJobClick: (WorkflowJob) -> Unit,
) {
    Column(Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusIcon(run.status, run.conclusion)
            Column(Modifier.weight(1f)) {
                Text(
                    run.displayTitle ?: run.name ?: "Workflow run",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val details = buildList {
                    add("#${run.runNumber}")
                    run.name?.let { add(it) }
                    run.headBranch?.let { add(it) }
                    run.event?.let { add(it) }
                }.joinToString(" · ")
                Text(
                    details,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                run.createdAt?.let {
                    Text(
                        it.take(16).replace('T', ' ') + " UTC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null
            )
        }

        if (expanded) {
            Column(
                modifier = Modifier.padding(start = 52.dp, end = 16.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (run.isActive) {
                        OutlinedButton(onClick = onCancel) { Text("Cancel") }
                    } else {
                        OutlinedButton(onClick = onRerun) { Text("Re-run") }
                        if (run.conclusion == "failure") {
                            OutlinedButton(onClick = onRerunFailed) { Text("Re-run failed") }
                        }
                    }
                }

                when {
                    jobs == null -> LinearProgressIndicator(Modifier.fillMaxWidth())
                    jobs.isEmpty() -> Text(
                        "No jobs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    else -> jobs.forEach { job -> JobRow(job, onClick = { onJobClick(job) }) }
                }
            }
        }
    }
}

@Composable
private fun JobRow(job: WorkflowJob, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusIcon(job.status, job.conclusion, size = 18.dp)
            Text(
                job.name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Logs",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        job.steps.forEach { step ->
            val symbol = when (step.conclusion) {
                "success" -> "✓"
                "failure" -> "✗"
                "skipped" -> "–"
                else -> if (step.status == "in_progress") "…" else "·"
            }
            Text(
                "$symbol ${step.name}",
                style = MaterialTheme.typography.bodySmall,
                color = if (step.conclusion == "failure") MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 28.dp)
            )
        }
    }
}

@Composable
private fun StatusIcon(status: String?, conclusion: String?, size: Dp = 22.dp) {
    if (status == "in_progress") {
        CircularProgressIndicator(Modifier.size(size), strokeWidth = 2.5.dp)
        return
    }
    if (status != null && status != "completed") {
        Icon(
            Icons.Filled.Schedule,
            contentDescription = "Queued",
            modifier = Modifier.size(size),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val (icon, tint) = when (conclusion) {
        "success" -> Icons.Filled.CheckCircle to SuccessGreen
        "failure", "timed_out", "startup_failure" -> Icons.Filled.Cancel to MaterialTheme.colorScheme.error
        else -> Icons.Filled.Info to MaterialTheme.colorScheme.outline
    }
    Icon(icon, contentDescription = conclusion, modifier = Modifier.size(size), tint = tint)
}

@Composable
private fun LogDialog(state: ActionsViewModel.LogState, onDismiss: () -> Unit) {
    val logText = state.text
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            when {
                state.failed -> Text(
                    "Couldn't load this log. Logs expire after a while and need a token with access to the repository."
                )
                logText == null -> CircularProgressIndicator()
                else -> {
                    val vertical = rememberScrollState()
                    val horizontal = rememberScrollState()
                    LaunchedEffect(vertical.maxValue) { vertical.scrollTo(vertical.maxValue) }
                    Box(
                        Modifier
                            .heightIn(max = 420.dp)
                            .verticalScroll(vertical)
                            .horizontalScroll(horizontal)
                    ) {
                        Text(
                            logText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            softWrap = false
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
