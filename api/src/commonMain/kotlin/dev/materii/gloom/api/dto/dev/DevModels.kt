package dev.materii.gloom.api.dto.dev

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserSearchResponse(
    val items: List<UserSummary> = emptyList(),
)

@Serializable
data class UserSummary(
    val login: String,
    @SerialName("avatar_url") val avatarUrl: String = "",
    val type: String = "User",
)

@Serializable
data class CommitItem(
    val sha: String = "",
    val commit: CommitDetail = CommitDetail(),
    val author: CommitUser? = null,
)

@Serializable
data class CommitDetail(
    val message: String = "",
    val author: CommitPerson = CommitPerson(),
)

@Serializable
data class CommitPerson(
    val name: String = "",
    val date: String? = null,
)

@Serializable
data class CommitUser(
    val login: String = "",
    @SerialName("avatar_url") val avatarUrl: String = "",
)

/** One week of /stats/commit_activity: [days] is Sunday..Saturday. */
@Serializable
data class CommitActivityWeek(
    val days: List<Int> = emptyList(),
    val total: Int = 0,
    val week: Long = 0,
)
