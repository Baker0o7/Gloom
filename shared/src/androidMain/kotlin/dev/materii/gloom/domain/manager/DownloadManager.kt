package dev.materii.gloom.domain.manager

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.benasher44.uuid.uuid4
import dev.materii.gloom.domain.manager.enums.DownloadHandlerPreference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import android.app.DownloadManager as AndroidDownloadManager

actual class DownloadManager(
    private val context: Context,
    private val authManager: AuthManager,
    private val prefs: PreferenceManager
) {

    private val downloadManager = context.getSystemService<AndroidDownloadManager>()!!
    private val downloadScope = CoroutineScope(Dispatchers.IO)
    private val gloomDownloadFolder = File(
        Environment.getExternalStorageDirectory(),
        Environment.DIRECTORY_DOWNLOADS
    ).resolve("Gloom")

    actual fun download(url: String, mimeType: String?, block: (String?) -> Unit) {
        if (
            prefs.downloadHandler == DownloadHandlerPreference.EXTERNAL &&
            openExternalDownloader(url, mimeType)
        ) {
            Handler(Looper.getMainLooper()).post { block(null) }
            return
        }

        val name = url.toUri().lastPathSegment ?: "${uuid4()}.blob"
        downloadScope.launch {
            download(url, File(gloomDownloadFolder, name)).also {
                Handler(Looper.getMainLooper()).post {
                    if (it.exists()) {
                        block(it.absolutePath)
                    }
                }
            }
        }
    }

    private fun openExternalDownloader(url: String, mimeType: String?): Boolean = runCatching {
        val uri = url.toUri()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (mimeType.isNullOrBlank()) {
                data = uri
            } else {
                setDataAndType(uri, mimeType)
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, null).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }.isSuccess

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    suspend fun download(url: String, out: File): File {
        out.parentFile?.mkdirs()

        return suspendCoroutine { continuation ->
            val receiver = object: BroadcastReceiver() {
                var dlId = 0L

                @SuppressLint("Range")
                override fun onReceive(context: Context, intent: Intent) {
                    val id = intent.getLongExtra(AndroidDownloadManager.EXTRA_DOWNLOAD_ID, -1)

                    if (dlId != id) return

                    val (status, reason) = AndroidDownloadManager.Query().run {
                        setFilterById(dlId)

                        val cursor = downloadManager.query(this)
                            .apply { moveToFirst() }

                        if (cursor.count == 0) {
                            -1 to -1
                        } else {
                            val status =
                                cursor.run { getInt(getColumnIndex(AndroidDownloadManager.COLUMN_STATUS)) }
                            val reason =
                                cursor.run { getInt(getColumnIndex(AndroidDownloadManager.COLUMN_REASON)) }

                            status to reason
                        }
                    }

                    when (status) {
                        -1                                       -> {
                            context.unregisterReceiver(this)
                            continuation.resumeWithException(Error("Download canceled"))
                        }

                        AndroidDownloadManager.STATUS_SUCCESSFUL -> {
                            context.unregisterReceiver(this)
                            continuation.resume(out)
                        }

                        AndroidDownloadManager.STATUS_FAILED     -> {
                            context.unregisterReceiver(this)
                            continuation.resumeWithException(
                                Error("Failed to download $url")
                            )
                        }

                        else                                     -> {}
                    }
                }
            }

            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    IntentFilter(AndroidDownloadManager.ACTION_DOWNLOAD_COMPLETE),
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(
                    receiver,
                    IntentFilter(AndroidDownloadManager.ACTION_DOWNLOAD_COMPLETE)
                )
            }

            receiver.dlId = AndroidDownloadManager.Request(url.toUri()).run {
                setTitle("Gloom: Downloading ${out.name}")
                setNotificationVisibility(AndroidDownloadManager.Request.VISIBILITY_VISIBLE)
                setDestinationUri(out.toUri())
                addRequestHeader("Authorization", authManager.authToken)
                downloadManager.enqueue(this)
            }
        }
    }
}
