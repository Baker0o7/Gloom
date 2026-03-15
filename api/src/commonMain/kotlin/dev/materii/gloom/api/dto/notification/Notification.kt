package dev.materii.gloom.api.dto.notification

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    val unread: Boolean,
    val reason: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_read_at") val lastReadAt: String? = null,
    val subject: NotificationSubjectDto,
    val repository: NotificationRepoDto,
)

@Serializable
data class NotificationSubjectDto(
    val title: String,
    val url: String? = null,
    val type: String,
)

@Serializable
data class NotificationRepoDto(
    @SerialName("full_name") val fullName: String,
    val owner: NotificationRepoOwnerDto,
)

@Serializable
data class NotificationRepoOwnerDto(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String,
)
