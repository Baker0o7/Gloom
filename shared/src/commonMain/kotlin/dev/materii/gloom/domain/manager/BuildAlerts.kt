package dev.materii.gloom.domain.manager

/**
 * Opt-in alerts for failed GitHub Actions runs. Implemented per platform;
 * when no implementation is registered the UI simply hides the control.
 */
interface BuildAlerts {
    fun isWatching(owner: String, name: String): Boolean

    /** "owner/name" of every repository with alerts turned on. */
    fun watchedRepos(): List<String>

    fun setWatching(owner: String, name: String, watch: Boolean)

    /** Asks for the notification permission if the platform needs one. */
    fun requestPermissionIfNeeded()
}
