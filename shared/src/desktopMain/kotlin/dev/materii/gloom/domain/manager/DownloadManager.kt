package dev.materii.gloom.domain.manager

actual class DownloadManager {

    actual fun download(url: String, block: (String) -> Unit) {
        // Desktop download not implemented
    }

}