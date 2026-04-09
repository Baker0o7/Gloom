package dev.materii.gloom.domain.manager

expect class DownloadManager {

    fun download(url: String, mimeType: String? = null, block: (String?) -> Unit = {})
}
