package com.mineinabyss.keepup.downloads.rclone

import com.lordcodes.turtle.ShellCommandNotFoundException
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
        val downloadPath = runCatching { Rclone.sync(source.query, targetDir) }
            .onFailure {
                if (it is ShellCommandNotFoundException)
                    return listOf(DownloadResult.Failure("rclone command not found", source.keyInConfig))
            }
            .getOrThrow()
        return listOf(DownloadResult.Downloaded(downloadPath, source.keyInConfig, overrideInfoMsg = MSG.rclone))
    }
}
