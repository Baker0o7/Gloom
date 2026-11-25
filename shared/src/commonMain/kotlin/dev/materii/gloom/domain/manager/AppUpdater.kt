package dev.materii.gloom.domain.manager

/**
 * Platform side of the in-app updater: download an APK and hand it to the installer.
 * Only registered on Android; the UI hides the feature when no implementation exists.
 */
interface AppUpdater {
    /** Asset-name suffix matching the installed build, e.g. "-release.apk" or "-debug.apk". */
    val apkSuffix: String

    /** Whether the OS currently lets this app launch the package installer. */
    fun canInstall(): Boolean

    /** Opens the system page where the user grants "install unknown apps" to this app. */
    fun openInstallSettings()

    /** Downloads [url] and returns the local file path. [onProgress] gets 0f..1f. */
    suspend fun download(url: String, fileName: String, onProgress: (Float) -> Unit): String

    fun install(path: String)
}
