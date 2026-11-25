package dev.materii.gloom.ui.screen.update.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.materii.gloom.api.dto.release.LatestRelease
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AppUpdater
import dev.materii.gloom.util.VersionName
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class UpdateViewModel(
    private val githubRepo: GithubRepository,
    private val updater: AppUpdater?,
) : ScreenModel {

    enum class Phase { IDLE, CHECKING, UP_TO_DATE, AVAILABLE, DOWNLOADING, READY, ERROR }

    val supported: Boolean get() = updater != null

    var phase by mutableStateOf(Phase.IDLE)
        private set
    var latest by mutableStateOf<LatestRelease?>(null)
        private set
    var progress by mutableStateOf(0f)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Set when the OS blocked the install until "install unknown apps" is granted. */
    var needsPermission by mutableStateOf(false)
        private set

    private var apkPath: String? = null

    fun check() {
        if (phase == Phase.CHECKING || phase == Phase.DOWNLOADING) return
        screenModelScope.launch {
            phase = Phase.CHECKING
            error = null
            when (val response = githubRepo.getLatestRelease(OWNER, REPO)) {
                is ApiResponse.Success -> {
                    latest = response.data
                    phase = if (isNewer(response.data.tagName, VersionName)) Phase.AVAILABLE else Phase.UP_TO_DATE
                }
                is ApiResponse.Error -> fail(response.error.message)
                is ApiResponse.Empty -> fail("No releases found")
                is ApiResponse.Failure -> fail("Couldn't reach GitHub. Check your connection.")
            }
        }
    }

    fun startUpdate() {
        val updater = updater ?: return
        val release = latest ?: return
        val asset = release.assets.firstOrNull { it.name.endsWith(updater.apkSuffix) }
        if (asset == null) {
            fail("This release has no ${updater.apkSuffix.removePrefix("-")} file for your build")
            return
        }
        if (phase == Phase.DOWNLOADING) return
        screenModelScope.launch {
            phase = Phase.DOWNLOADING
            progress = 0f
            error = null
            try {
                apkPath = updater.download(asset.browserDownloadUrl, asset.name) { progress = it }
                phase = Phase.READY
                install()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                fail(e.message ?: "Download failed")
            }
        }
    }

    fun install() {
        val updater = updater ?: return
        val path = apkPath ?: return
        if (!updater.canInstall()) {
            needsPermission = true
            updater.openInstallSettings()
            return
        }
        needsPermission = false
        runCatching { updater.install(path) }.onFailure { fail(it.message ?: "Couldn't open the installer") }
    }

    private fun fail(message: String?) {
        error = message ?: "Something went wrong"
        phase = Phase.ERROR
    }

    companion object {
        const val OWNER = "Baker0o7"
        const val REPO = "Gloom"

        private fun parts(version: String): List<Int> =
            Regex("\\d+").findAll(version.substringBefore('-').removePrefix("v")).map { it.value.toInt() }.toList()

        /** True when [latest] (e.g. "v1.0.95") is a higher version than [current] (e.g. "1.0.94-debug"). */
        fun isNewer(latest: String, current: String): Boolean {
            val a = parts(latest)
            val b = parts(current)
            if (a.isEmpty() || b.isEmpty()) return false
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
