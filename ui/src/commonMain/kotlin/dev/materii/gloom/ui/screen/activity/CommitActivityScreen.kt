package dev.materii.gloom.ui.screen.activity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import dev.materii.gloom.api.dto.dev.CommitActivityWeek
import dev.materii.gloom.api.dto.dev.CommitItem
import dev.materii.gloom.ui.component.LoadStateBox
import dev.materii.gloom.ui.component.ScreenScaffold
import dev.materii.gloom.ui.screen.activity.viewmodel.CommitActivityViewModel
import dev.materii.gloom.util.TimeUtils
import dev.materii.gloom.util.format
import kotlinx.datetime.Instant
import org.koin.core.parameter.parametersOf

class CommitActivityScreen(
    private val owner: String,
    private val name: String,
) : Screen {

    override val key: ScreenKey get() = "$owner/$name/commit-activity"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = koinScreenModel<CommitActivityViewModel> {
            parametersOf(CommitActivityViewModel.Input(owner, name))
        }
        val days = remember(viewModel.commits.size) {
            viewModel.commits.groupBy { it.commit.author.date?.take(10) ?: "" }.entries.toList()
        }

        ScreenScaffold(title = "Commit activity", subtitle = "$owner/$name") { pv ->
            PullToRefreshBox(
                isRefreshing = viewModel.isLoading && viewModel.commits.isNotEmpty(),
                onRefresh = { viewModel.load() },
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
            ) {
                LoadStateBox(
                    isLoading = viewModel.isLoading,
                    hasError = viewModel.hasError,
                    isEmpty = viewModel.commits.isEmpty(),
                    emptyText = "No commits found",
                    onRetry = { viewModel.load() }
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        if (viewModel.weeks.isNotEmpty()) {
                            item("chart") { ActivityChart(viewModel.weeks) }
                        }

                        days.forEach { (day, dayCommits) ->
                            item("day_$day") {
                                val label = remember(day) {
                                    dayCommits.first().commit.author.date
                                        ?.let { runCatching { Instant.parse(it).format("EEE, MMM d") }.getOrNull() }
                                        ?: day
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
                                )
                            }
                            items(dayCommits, key = { it.sha }) { commit -> TimelineItem(commit) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityChart(weeks: List<CommitActivityWeek>) {
    val total = remember(weeks) { weeks.sumOf { it.total } }
    val busiest = remember(weeks) { weeks.maxByOrNull { it.total } }
    val barColor = MaterialTheme.colorScheme.primary

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$total commits in the last year", style = MaterialTheme.typography.titleMedium)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val count = weeks.size
            val gap = 2.dp.toPx()
            val barWidth = (size.width - gap * (count - 1)) / count
            val max = (weeks.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
            weeks.forEachIndexed { index, week ->
                val height = size.height * (week.total.toFloat() / max).coerceAtLeast(0.04f)
                drawRect(
                    color = barColor.copy(alpha = if (week.total == 0) 0.2f else 1f),
                    topLeft = Offset(index * (barWidth + gap), size.height - height),
                    size = Size(barWidth, height)
                )
            }
        }
        if (busiest != null && busiest.total > 0) {
            val label = runCatching { Instant.fromEpochSeconds(busiest.week).format("MMM d") }.getOrNull()
            Text(
                "Busiest week${if (label != null) " (of $label)" else ""}: ${busiest.total} commits",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimelineItem(commit: CommitItem) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    val time = remember(commit.sha) {
        commit.commit.author.date?.let { runCatching { TimeUtils.getTimeSince(Instant.parse(it)) }.getOrNull() }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp)
    ) {
        Box(
            Modifier
                .width(24.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                    drawCircle(dotColor, radius = 5.dp.toPx(), center = Offset(x, 14.dp.toPx()))
                }
        )
        Column(Modifier.padding(start = 8.dp, top = 4.dp, bottom = 12.dp)) {
            Text(
                commit.commit.message.lineSequence().firstOrNull().orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val author = commit.author?.login?.takeIf { it.isNotEmpty() } ?: commit.commit.author.name
            Text(
                buildString {
                    append(author)
                    append(" · ")
                    append(commit.sha.take(7))
                    if (time != null) append(" · $time")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
