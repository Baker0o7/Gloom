package dev.materii.gloom.api.dto.actions

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowRunsResponse(
    @SerialName("workflow_runs") val runs: List<WorkflowRun> = emptyList(),
)

@Serializable
data class WorkflowRun(
    val id: Long,
    val name: String? = null,
    @SerialName("display_title") val displayTitle: String? = null,
    val status: String? = null,
    val conclusion: String? = null,
    @SerialName("head_branch") val headBranch: String? = null,
    @SerialName("head_sha") val headSha: String = "",
    val event: String? = null,
    @SerialName("run_number") val runNumber: Int = 0,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
) {
    val isActive: Boolean get() = status != null && status != "completed"
}

@Serializable
data class WorkflowJobsResponse(
    val jobs: List<WorkflowJob> = emptyList(),
)

@Serializable
data class WorkflowJob(
    val id: Long,
    val name: String = "",
    val status: String? = null,
    val conclusion: String? = null,
    val steps: List<WorkflowStep> = emptyList(),
)

@Serializable
data class WorkflowStep(
    val name: String = "",
    val status: String? = null,
    val conclusion: String? = null,
    val number: Int = 0,
)
