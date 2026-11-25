package dev.materii.gloom.util.update

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import dev.materii.gloom.domain.manager.AppUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class AndroidAppUpdater(private val context: Context) : AppUpdater {

    override val apkSuffix: String =
        if (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) "-debug.apk" else "-release.apk"

    override fun canInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    override fun openInstallSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    override suspend fun download(url: String, fileName: String, onProgress: (Float) -> Unit): String =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply {
                mkdirs()
                listFiles()?.forEach { it.delete() } // drop older downloads
            }
            val out = File(dir, fileName)
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.instanceFollowRedirects = true
                val code = connection.responseCode
                if (code !in 200..299) error("Download failed (HTTP $code)")
                val total = connection.contentLengthLong
                connection.inputStream.use { input ->
                    out.outputStream().use { output ->
                        val buffer = ByteArray(32 * 1024)
                        var read = 0L
                        var lastReported = -1
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            ensureActive()
                            if (total > 0) {
                                val percent = (read * 100 / total).toInt()
                                if (percent != lastReported) {
                                    lastReported = percent
                                    onProgress(read.toFloat() / total)
                                }
                            }
                        }
                    }
                }
                if (out.length() == 0L) error("Downloaded file is empty")
                out.absolutePath
            } catch (t: Throwable) {
                out.delete()
                throw t
            } finally {
                connection.disconnect()
            }
        }

    override fun install(path: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", File(path))
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
