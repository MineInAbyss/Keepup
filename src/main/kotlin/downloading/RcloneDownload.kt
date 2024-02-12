package downloading

import commands.Rclone
import helpers.MSG
import java.nio.file.Path

class RcloneDownload(
    val source: Source,
    val targetDir: Path,
) : Downloader {
    override suspend fun download(): List<DownloadResult> {
        val downloadPath = Rclone.sync(source.query, targetDir)
        return listOf(DownloadResult.Downloaded(downloadPath, source.keyInConfig, overrideInfoMsg = MSG.rclone))
    }
}
