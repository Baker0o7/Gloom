package dev.materii.gloom.util.alerts

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.materii.gloom.domain.manager.BuildAlerts
import java.util.concurrent.TimeUnit

/** Set by the activity so non-UI code can trigger the runtime permission prompt. */
object NotificationPermission {
    var request: (() -> Unit)? = null
}

class AndroidBuildAlerts(private val context: Context) : BuildAlerts {

    private val prefs get() = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun isWatching(owner: String, name: String): Boolean =
        watched(context).contains("$owner/$name")

    override fun watchedRepos(): List<String> = watched(context).sorted()

    override fun setWatching(owner: String, name: String, watch: Boolean) {
        val repos = watched(context).toMutableSet()
        if (watch) repos.add("$owner/$name") else repos.remove("$owner/$name")
        prefs.edit().putStringSet(KEY_REPOS, repos).apply()

        val work = WorkManager.getInstance(context)
        if (repos.isEmpty()) {
            work.cancelUniqueWork(WORK_NAME)
        } else {
            val request = PeriodicWorkRequestBuilder<BuildAlertWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    override fun requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) NotificationPermission.request?.invoke()
    }

    companion object {
        const val PREFS = "build_alerts"
        const val KEY_REPOS = "repos"
        const val WORK_NAME = "build_failure_alerts"

        fun watched(context: Context): Set<String> =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getStringSet(KEY_REPOS, emptySet()) ?: emptySet()
    }
}
