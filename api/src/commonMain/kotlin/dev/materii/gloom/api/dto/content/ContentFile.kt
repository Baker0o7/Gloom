package dev.materii.gloom.api.dto.content

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of GET /repos/{owner}/{repo}/contents/{path} for a single file. */
@Serializable
data class ContentFile(
    val name: String = "",
    val path: String = "",
    val sha: String = "",
    val size: Long = 0,
    val content: String = "",
    val encoding: String = "",
)

/** Response of GET /repos/{owner}/{repo}/git/ref/heads/{branch}. */
@Serializable
data class GitRef(
    @SerialName("object") val target: GitRefObject,
)

@Serializable
data class GitRefObject(
    val sha: String,
)
