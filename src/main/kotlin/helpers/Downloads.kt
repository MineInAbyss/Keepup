package helpers

import GithubDownload
import Keepup
import commands.DownloadedItem
import commands.Wget
import java.nio.file.Path

/**
 * Downloads a file from a [source] definition into a [targetDir]
 *
 * @param source Takes form of an https url, rclone remote, or `.` to ignore
 */
context(Keepup)
fun download(source: String, targetDir: Path): List<DownloadedItem> = when {
    source == "." -> emptyList()
    source.startsWith("github:") -> GithubDownload.from(source).download(targetDir, overrideGithubRelease)
    source.matches("^https?://.*".toRegex()) -> listOfNotNull(Wget(source, targetDir))
    else -> listOfNotNull(commands.Rclone.sync(source, targetDir))
}
