package dev.materii.gloom.api.dto.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String,
    val unread: Boolean,
    val reason: String,
    val subject: NotificationSubject,
    val repository: NotificationRepository,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class NotificationSubject(
    val title: String,
    val type: String,           // "Issue" | "PullRequest" | "Release" | "Commit" | …
    val url: String? = null,
    @SerialName("latest_comment_url") val latestCommentUrl: String? = null,
)

@Serializable
data class NotificationRepository(
    val id: Long,
    @SerialName("full_name") val fullName: String,
    val name: String,
    val owner: NotificationOwner,
)

@Serializable
data class NotificationOwner(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String,
)
