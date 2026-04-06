package dev.materii.gloom.api.model

import dev.materii.gloom.api.dto.user.User
import dev.materii.gloom.gql.*
import dev.materii.gloom.gql.fragment.Contributions
import dev.materii.gloom.gql.fragment.OrgSponsoringFragment
import dev.materii.gloom.gql.fragment.Social
import dev.materii.gloom.gql.fragment.UserSponsoringFragment
import dev.materii.gloom.gql.type.SocialAccountProvider
import kotlinx.datetime.LocalDateTime

data class ModelUser(
    val username: String? = null,
    val displayName: String? = null,
    val bio: String? = null,
    val id: String? = null,
    val nodeId: String? = null,
    val avatar: String? = null,
    val gravatarId: String? = null,
    val readme: String? = null,
    val type: User.Type,
    val admin: Boolean? = null,
    val company: String? = null,
    val website: String? = null,
    val location: String? = null,
    val email: String? = null,
    val hireable: Boolean? = null,
    val socials: List<Social> = emptyList(),
    val repos: Long? = null,
    val gists: Long? = null,
    val followers: Long? = null,
    val following: Long? = null,
    val sponsoring: Long? = null,
    val joined: LocalDateTime? = null,
    val updated: LocalDateTime? = null,
    val status: ModelStatus? = null,
    val starred: Long? = null,
    val orgs: Long? = null,
    val isMember: Boolean? = null,
    val pinnedItems: List<dev.materii.gloom.api.model.Pinnable?> = emptyList(),
    val canFollow: Boolean? = null,
    val isFollowing: Boolean? = null,
    val isSupporter: Boolean = false,
    val isFollowingYou: Boolean = false,
    val contributions: Contributions? = null
) {

    companion object {

        fun fromApi(apiUser: User) = with(apiUser) {
            ModelUser(
                username = username,
                displayName = displayName,
                bio = bio,
                id = id.toString(),
                nodeId = nodeId,
                avatar = avatar,
                gravatarId = gravatarId,
                type = type,
                admin = admin,
                company = company,
                website = website,
                location = location,
                email = email,
                hireable = hireable,
                socials = listOf(
                    Social(
                        "@$twitterUsername",
                        SocialAccountProvider.TWITTER,
                        "https://twitter.com/$twitterUsername"
                    )
                ),
                repos = repos,
                gists = gists,
                followers = followers,
                following = following,
                joined = joined,
                updated = updated
            )
        }

        fun fromProfileQuery(pq: ProfileQuery.Data) = with(pq) {
            with(viewer.userProfile) {
                ModelUser(
                    id = id,
                    username = login,
                    displayName = name,
                    bio = bio,
                    avatar = avatarUrl.toString(),
                    readme = profileReadme?.contentHTML?.toString(),
                    type = User.Type.USER,
                    company = company,
                    website = websiteUrl.toString(),
                    socials = socialAccounts.nodes?.mapNotNull { it?.social } ?: emptyList(),
                    repos = repositories.totalCount.toLong(),
                    orgs = organizations.totalCount.toLong(),
                    starred = starredRepositories.totalCount.toLong(),
                    sponsoring = sponsoring.totalCount.toLong(),
                    followers = followers.totalCount.toLong(),
                    following = following.totalCount.toLong(),
                    status = ModelStatus.fromProfileQuery(pq),
                    email = email,
                    location = location,
                    pinnedItems = pinnedItems.nodes?.map { ModelRepo.fromPinnedRepo(it?.pinnedRepo) }
                        ?: emptyList(),
                    canFollow = viewerCanFollow,
                    isFollowing = viewerIsFollowing,
                    isSupporter = user?.viewerIsSponsoring ?: false,
                    isFollowingYou = isFollowingViewer,
                    contributions = contributionsCollection.contributions
                )
            }
        }

        fun fromUserProfileQuery(upq: UserProfileQuery.Data): ModelUser {
            val isUser = upq.repositoryOwner?.userProfile != null
            val isOrg = upq.repositoryOwner?.orgProfile != null && !isUser

            if (isUser) {
                val userProfile = upq.repositoryOwner?.userProfile
                if (userProfile != null) {
                    return ModelUser(
                        id = userProfile.id,
                        username = userProfile.login,
                        displayName = userProfile.name,
                        bio = userProfile.bio,
                        avatar = userProfile.avatarUrl.toString(),
                        readme = userProfile.profileReadme?.contentHTML?.toString(),
                        type = User.Type.USER,
                        company = userProfile.company,
                        website = userProfile.websiteUrl.toString(),
                        socials = userProfile.socialAccounts.nodes?.mapNotNull { it?.social } ?: emptyList(),
                        repos = userProfile.repositories.totalCount.toLong(),
                        orgs = userProfile.organizations.totalCount.toLong(),
                        starred = userProfile.starredRepositories.totalCount.toLong(),
                        sponsoring = userProfile.sponsoring.totalCount.toLong(),
                        followers = userProfile.followers.totalCount.toLong(),
                        following = userProfile.following.totalCount.toLong(),
                        status = ModelStatus.fromUserProfileQuery(upq),
                        email = userProfile.email,
                        location = userProfile.location,
                        pinnedItems = userProfile.pinnedItems.nodes?.map { ModelRepo.fromPinnedRepo(it?.pinnedRepo) } ?: emptyList(),
                        canFollow = userProfile.viewerCanFollow,
                        isFollowing = userProfile.viewerIsFollowing,
                        isSupporter = false,
                        isFollowingYou = userProfile.isFollowingViewer,
                        contributions = userProfile.contributionsCollection.contributions
                    )
                }
            } else if (isOrg) {
                val orgProfile = upq.repositoryOwner?.orgProfile
                if (orgProfile != null) {
                    return ModelUser(
                        username = orgProfile.login,
                        displayName = orgProfile.name,
                        bio = orgProfile.bio,
                        avatar = orgProfile.avatarUrl.toString(),
                        readme = orgProfile.readme?.contentHTML?.toString(),
                        type = User.Type.ORG,
                        website = orgProfile.websiteUrl.toString(),
                        socials = orgProfile.twitterUsername?.let {
                            listOf(
                                Social(
                                    "@$it",
                                    SocialAccountProvider.TWITTER,
                                    "https://twitter.com/$it"
                                )
                            )
                        } ?: emptyList(),
                        email = orgProfile.publicEmail,
                        location = orgProfile.location,
                        repos = orgProfile.repositories.totalCount.toLong(),
                        sponsoring = orgProfile.sponsoring.totalCount.toLong(),
                        isMember = orgProfile.viewerIsAMember,
                        pinnedItems = orgProfile.pinnedItems.nodes?.map { ModelRepo.fromPinnedRepo(it?.pinnedRepo) }
                            ?: emptyList(),
                        isSupporter = false
                    )
                }
            } else {
                return ModelUser(type = User.Type.USER)
            }
        }

        return ModelUser(type = User.Type.USER)

        fun fromJoinedOrgsQuery(joq: JoinedOrgsQuery.Node) = with(joq.orgListOrgFragment) {
            ModelUser(
                username = login,
                displayName = name,
                bio = description,
                avatar = avatarUrl,
                type = User.Type.ORG
            )
        }

        fun fromFollowersQuery(followersQuery: FollowersQuery.Node) =
            with(followersQuery.userListUserFragment) {
                ModelUser(
                    username = login,
                    displayName = name,
                    bio = bio,
                    avatar = avatarUrl,
                    type = User.Type.USER
                )
            }

        fun fromFollowingQuery(followingQuery: FollowingQuery.Node) =
            with(followingQuery.userListUserFragment) {
                ModelUser(
                    username = login,
                    displayName = name,
                    bio = bio,
                    avatar = avatarUrl,
                    type = User.Type.USER
                )
            }

        fun fromSponsoringQuery(sponsoringQuery: UserSponsoringFragment.Node) =
            with(sponsoringQuery) {
                val isUser = sponsoringQuery.userListUserFragment != null
                val isOrg = sponsoringQuery.orgListOrgFragment != null && !isUser

                if (isUser) {
                    with(sponsoringQuery.userListUserFragment!!) {
                        ModelUser(
                            username = login,
                            displayName = name,
                            bio = bio,
                            avatar = avatarUrl,
                            type = User.Type.USER
                        )
                    }
                } else if (isOrg) {
                    with(sponsoringQuery.orgListOrgFragment!!) {
                        ModelUser(
                            username = login,
                            displayName = name,
                            bio = description,
                            avatar = avatarUrl,
                            type = User.Type.USER
                        )
                    }
                } else {
                    ModelUser(type = User.Type.USER)
                }
            }

        fun fromSponsoringQuery(sponsoringQuery: OrgSponsoringFragment.Node) =
            with(sponsoringQuery) {
                val isUser = sponsoringQuery.userListUserFragment != null
                val isOrg = sponsoringQuery.orgListOrgFragment != null && !isUser

                if (isUser) {
                    with(sponsoringQuery.userListUserFragment!!) {
                        ModelUser(
                            username = login,
                            displayName = name,
                            bio = bio,
                            avatar = avatarUrl,
                            type = User.Type.USER
                        )
                    }
                } else if (isOrg) {
                    with(sponsoringQuery.orgListOrgFragment!!) {
                        ModelUser(
                            username = login,
                            displayName = name,
                            bio = description,
                            avatar = avatarUrl,
                            type = User.Type.USER
                        )
                    }
                } else {
                    ModelUser(type = User.Type.USER)
                }
            }

        fun fromContributorsQuery(contributorsQuery: RepoContributorsQuery.Node): ModelUser {
            return with(contributorsQuery) {
                ModelUser(
                    username = login,
                    displayName = name,
                    bio = bio,
                    avatar = avatarUrl,
                    type = User.Type.USER
                )
            }
        }
    }

}