package dev.materii.gloom.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.benasher44.uuid.uuid4
import dev.icerock.moko.resources.compose.stringResource
import dev.materii.gloom.Res
import dev.materii.gloom.domain.manager.DownloadManager
import dev.materii.gloom.ui.widget.alert.LocalAlertController
import org.koin.compose.koinInject

@Composable
fun DownloadButton(
    downloadUrl: String,
    modifier: Modifier = Modifier,
    fileName: String = downloadUrl.split("/").lastOrNull() ?: "${uuid4()}.blob",
    mimeType: String? = null,
    onDownloadFinish: (String?) -> Unit = {}
) {
    val downloadManager: DownloadManager = koinInject()
    val alertController = LocalAlertController.current
    val downloadingText = stringResource(Res.strings.msg_downloading_file, fileName)
    val downloadedText = stringResource(Res.strings.msg_download_completed)
    val sentToExternalText = stringResource(Res.strings.msg_download_sent_external)

    IconButton(
        onClick = {
            alertController.showText(downloadingText, icon = Icons.Filled.Download)
            downloadManager.download(downloadUrl, mimeType) {
                val message = if (it == null) sentToExternalText else downloadedText
                alertController.showText(message, icon = Icons.Outlined.CheckCircleOutline)
                onDownloadFinish(it)
            }
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Filled.Download,
            contentDescription = stringResource(Res.strings.action_download)
        )
    }
}
