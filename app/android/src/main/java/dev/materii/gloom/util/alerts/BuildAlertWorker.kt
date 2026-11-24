package dev.materii.gloom.util.alerts

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.materii.gloom.api.repository.GithubRepository
import dev.materii.gloom.api.util.ApiResponse
import dev.materii.gloom.domain.manager.AuthManager
import org.koin.core.context.GlobalContext

/**
 * Periodically checks the latest finished run of every watched repository and posts a
 * notification when it failed. The first check only records a baseline, so enabling alerts
 * on a repo that is already red doesn't fire immediately.
 */
class BuildAlertWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val koin = GlobalContext.getOrNull() ?: return Result.success()
        val auth = koin.get<AuthManager>()
        if (!auth.isSignedIn) return Result.success()
        val repository = koin.get<GithubRepository>()

        val prefs = appContext.getSharedPreferences(AndroidBuildAlerts.PREFS, Context.MODE_PRIVATE)

        for (fullName in AndroidBuildAlerts.watched(appContext)) {
            val (owner, name) = fullName.split("/").let { it.getOrNull(0) to it.getOrNull(1) }
            if (owner == null || name == null) continue

            val response = repository.getWorkflowRuns(owner, name)
            if (response !is ApiResponse.Success) continue

            val latest = response.data.runs.firstOrNull { !it.isActive } ?: continue
            val lastSeen = prefs.getLong("last_$fullName", -1L)
            prefs.edit().putLong("last_$fullName", latest.id).apply()

            val failed = latest.conclusion == "failure" || latest.conclusion == "timed_out"
            if (lastSeen != -1L && latest.id != lastSeen && failed) {
                notifyFailure(fullName, latest.displayTitle ?: latest.name ?: "Workflow run", latest.headBranch, latest.id.toInt())
            }
        }
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notifyFailure(fullName: String, title: String, branch: String?, notificationId: Int) {
        val manager = NotificationManagerCompat.from(appContext)
        if (!manager.areNotificationsEnabled()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Build failures", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Alerts when a GitHub Actions run fails in a repository you watch"
            manager.createNotificationChannel(channel)
        }

        val launch = appContext.packageManager.getLaunchIntentForPackage(appContext.packageName)
        val pending = launch?.let {
            PendingIntent.getActivity(appContext, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Build failed: $fullName")
            .setContentText(if (branch != null) "$title on $branch" else title)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }

    private companion object {
        const val CHANNEL_ID = "build_failures"
    }
}
