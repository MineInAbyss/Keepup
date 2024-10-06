package com.mineinabyss.keepup.downloads.rclone

import com.mineinabyss.keepup.downloads.DownloadResult
import com.mineinabyss.keepup.downloads.Downloader
import com.mineinabyss.keepup.downloads.parsing.DownloadSource
import com.mineinabyss.keepup.helpers.MSG
import java.nio.file.Path

class RcloneDownloader(
    val source: DownloadSource,
    val targetDir: Path,
) : Downloader {
    override suspend fun download(): List<DownloadResult> {
        val downloadPath = Rclone.sync(source.query, targetDir)
        return listOf(DownloadResult.Downloaded(downloadPath, source.keyInConfig, overrideInfoMsg = MSG.rclone))
    }
}
