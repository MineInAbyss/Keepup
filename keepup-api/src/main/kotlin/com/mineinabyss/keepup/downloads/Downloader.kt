package com.mineinabyss.keepup.downloads

interface Downloader {
    suspend fun download(): List<DownloadResult>
}
