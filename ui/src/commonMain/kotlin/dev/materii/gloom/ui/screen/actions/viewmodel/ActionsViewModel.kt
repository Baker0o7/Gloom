package dev.materii.gloom.ui.screen.actions.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.actions.WorkflowJob
import dev.materii.gloom.api.dto.actions.WorkflowRun
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ActionsViewModel(
    private val githubRepo: GithubRepository,
    private val input: Input,
) : ScreenModel {

    data class Input(val owner: String, val name: String)

    /** [text] == null && !failed means the log is still loading. */
    data class LogState(val title: String, val text: String? = null, val failed: Boolean = false)

    val runs = mutableStateListOf<WorkflowRun>()
    val jobs = mutableStateMapOf<Long, List<WorkflowJob>>()

    var isLoading by mutableStateOf(false)
        private set

    var hasError by mutableStateOf(false)
        private set

    var expandedRunId by mutableStateOf<Long?>(null)
        private set

    var message by mutableStateOf<String?>(null)
    var logState by mutableStateOf<LogState?>(null)
        private set

    private var loadJob: Job? = null

    init {
        load()
        startPolling()
    }

    fun load(silent: Boolean = false) {
        if (loadJob?.isActive == true) return
        loadJob = screenModelScope.launch {
            if (!silent) isLoading = true
            when (val response = githubRepo.getWorkflowRuns(input.owner, input.name)) {
                is ApiResponse.Success -> {
                    hasError = false
                    runs.clear()
                    runs.addAll(response.data.runs)
                }
                is ApiResponse.Empty -> {
                    hasError = false
                    runs.clear()
                }
                is ApiResponse.Error -> if (runs.isEmpty()) hasError = true
                is ApiResponse.Failure -> if (runs.isEmpty()) hasError = true
            }
            expandedRunId?.let { fetchJobs(it) }
            isLoading = false
        }
    }

    /** Keep running builds fresh without the user having to pull to refresh. */
    private fun startPolling() {
        screenModelScope.launch {
            while (true) {
                delay(15_000)
                if (runs.any { it.isActive }) load(silent = true)
            }
        }
    }

    fun toggleExpanded(run: WorkflowRun) {
        if (expandedRunId == run.id) {
            expandedRunId = null
            return
        }
        expandedRunId = run.id
        screenModelScope.launch { fetchJobs(run.id) }
    }

    private suspend fun fetchJobs(runId: Long) {
        val response = githubRepo.getRunJobs(input.owner, input.name, runId)
        if (response is ApiResponse.Success) jobs[runId] = response.data.jobs
        else if (jobs[runId] == null) jobs[runId] = emptyList()
    }

    fun cancel(run: WorkflowRun) =
        perform("Cancel requested") { githubRepo.cancelRun(input.owner, input.name, run.id) }

    fun rerun(run: WorkflowRun) =
        perform("Re-run started") { githubRepo.rerunRun(input.owner, input.name, run.id) }

    fun rerunFailed(run: WorkflowRun) =
        perform("Re-running failed jobs") { githubRepo.rerunFailedJobs(input.owner, input.name, run.id) }

    private fun perform(successMessage: String, call: suspend () -> ApiResponse<String>) {
        screenModelScope.launch {
            when (call()) {
                is ApiResponse.Success, is ApiResponse.Empty -> {
                    message = successMessage
                    delay(2_000)
                    load(silent = true)
                }
                is ApiResponse.Error ->
                    message = "Action failed. Your token may need the repo and workflow permissions."
                is ApiResponse.Failure ->
                    message = "Network error. Please try again."
            }
        }
    }

    fun showLogs(job: WorkflowJob) {
        logState = LogState(job.name)
        screenModelScope.launch {
            logState = when (val response = githubRepo.getJobLogs(input.owner, input.name, job.id)) {
                is ApiResponse.Success -> LogState(job.name, text = formatLog(response.data))
                is ApiResponse.Empty -> LogState(job.name, text = "(empty log)")
                is ApiResponse.Error -> LogState(job.name, failed = true)
                is ApiResponse.Failure -> LogState(job.name, failed = true)
            }
        }
    }

    fun dismissLogs() {
        logState = null
    }

    private fun formatLog(raw: String): String {
        val timestamp = Regex("^\\d{4}-\\d{2}-\\d{2}T[\\d:.]+Z ")
        val lines = raw.lines().map { it.replace(timestamp, "") }
        return if (lines.size > MAX_LOG_LINES)
            "… showing the last $MAX_LOG_LINES of ${lines.size} lines\n" + lines.takeLast(MAX_LOG_LINES).joinToString("\n")
        else lines.joinToString("\n")
    }

    private companion object {
        const val MAX_LOG_LINES = 500
    }
}
