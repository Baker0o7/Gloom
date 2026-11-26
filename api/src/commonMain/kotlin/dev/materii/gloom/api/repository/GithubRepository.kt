package dev.materii.gloom.api.repository

import dev.materii.gloom.api.service.GithubApiService
import dev.materii.gloom.api.util.getOrNull

class GithubRepository(
    private val service: GithubApiService,
) {

    suspend fun getNotifications(all: Boolean = false, page: Int = 1) =
        service.getNotifications(all = all, page = page)

    suspend fun markThreadRead(threadId: String) =
        service.markThreadRead(threadId)

    suspend fun markAllRead() =
        service.markAllRead()

    suspend fun createIssue(owner: String, repo: String, title: String, body: String) =
        service.createIssue(owner, repo, title, body)

    suspend fun getWorkflowRuns(owner: String, repo: String) = service.getWorkflowRuns(owner, repo)
    suspend fun getRunJobs(owner: String, repo: String, runId: Long) = service.getRunJobs(owner, repo, runId)
    suspend fun cancelRun(owner: String, repo: String, runId: Long) = service.cancelRun(owner, repo, runId)
    suspend fun rerunRun(owner: String, repo: String, runId: Long) = service.rerunRun(owner, repo, runId)
    suspend fun rerunFailedJobs(owner: String, repo: String, runId: Long) = service.rerunFailedJobs(owner, repo, runId)
    suspend fun getJobLogs(owner: String, repo: String, jobId: Long) = service.getJobLogs(owner, repo, jobId)

    suspend fun getFileContent(owner: String, repo: String, path: String, ref: String) =
        service.getFileContent(owner, repo, path, ref)

    suspend fun updateFile(
        owner: String, repo: String, path: String, message: String,
        contentBase64: String, sha: String, branch: String,
    ) = service.updateFile(owner, repo, path, message, contentBase64, sha, branch)

    suspend fun getBranchRef(owner: String, repo: String, branch: String) =
        service.getBranchRef(owner, repo, branch)

    suspend fun createBranch(owner: String, repo: String, newBranch: String, fromSha: String) =
        service.createBranch(owner, repo, newBranch, fromSha)

    suspend fun getGists() = service.getGists()
    suspend fun getListPage(endpoint: String, page: Int) = service.getListPage(endpoint, page)
    suspend fun getLatestRelease(owner: String, name: String) = service.getLatestRelease(owner, name)
    suspend fun getGist(id: String) = service.getGist(id)
    suspend fun createGist(description: String, isPublic: Boolean, fileName: String, content: String) =
        service.createGist(description, isPublic, fileName, content)
    suspend fun deleteGist(id: String) = service.deleteGist(id)
    suspend fun searchUsers(query: String) = service.searchUsers(query)
    suspend fun getCommits(owner: String, repo: String) = service.getCommits(owner, repo)
    suspend fun getCommitActivity(owner: String, repo: String) = service.getCommitActivity(owner, repo)
    suspend fun graphql(query: String, variables: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap())) =
        service.graphql(query, variables)
}