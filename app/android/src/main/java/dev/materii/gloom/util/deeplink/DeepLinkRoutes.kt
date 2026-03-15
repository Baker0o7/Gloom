package dev.materii.gloom.util.deeplink

import dev.materii.gloom.ui.screen.list.RepositoryListScreen
import dev.materii.gloom.ui.screen.list.StarredReposListScreen
import dev.materii.gloom.ui.screen.profile.ProfileScreen
import dev.materii.gloom.ui.screen.release.ReleaseScreen
import dev.materii.gloom.ui.screen.repo.RepoScreen

fun addAllRoutes() {
    with(DeepLinkHandler) {

        // ── User / Org profile ─────────────────────────────────────────────
        addOnLinkVisitedListener("/{username}") { (username), query ->
            when (query["tab"]) {
                "repositories" -> listOf(ProfileScreen(username), RepositoryListScreen(username))
                "stars"        -> listOf(ProfileScreen(username), StarredReposListScreen(username))
                else           -> listOf(ProfileScreen(username))
            }
        }

        // ── Repository root ────────────────────────────────────────────────
        addOnLinkVisitedListener("/{owner}/{repo}") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        // ── Issues / PRs / Actions / Settings → open repo ──────────────────
        addOnLinkVisitedListener("/{owner}/{repo}/issues") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/pulls") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/actions") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/issues/{number}") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/pull/{number}") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        // ── Commits / tree / blob → open repo ──────────────────────────────
        // (CommitsScreen needs a node ID not available from URL alone)
        addOnLinkVisitedListener("/{owner}/{repo}/commits") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/commits/{branch}") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/tree/{branch}") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/blob/{branch}") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }

        // ── Releases ───────────────────────────────────────────────────────
        addOnLinkVisitedListener("/{owner}/{repo}/releases/tag/{tag}") { (owner, repo, tag), _ ->
            listOf(ReleaseScreen(owner, repo, tag))
        }

        addOnLinkVisitedListener("/{owner}/{repo}/releases") { (owner, repo), _ ->
            listOf(RepoScreen(owner, repo))
        }
    }
}
