package dev.materii.gloom.api.dto.gist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Gist(
    val id: String,
    val description: String? = null,
    @SerialName("public") val isPublic: Boolean = true,
    @SerialName("html_url") val htmlUrl: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val files: Map<String, GistFile> = emptyMap(),
)

@Serializable
data class GistFile(
    val filename: String = "",
    val language: String? = null,
    val size: Long = 0,
    /** Only present when a single gist is fetched. */
    val content: String? = null,
    val truncated: Boolean = false,
)
