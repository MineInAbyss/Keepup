package com.mineinabyss.keepup.downloads.parsing

import com.mineinabyss.keepup.downloads.DownloadResult
import com.mineinabyss.keepup.downloads.Downloader
import com.mineinabyss.keepup.downloads.github.GithubArtifact
import com.mineinabyss.keepup.downloads.github.GithubConfig
import com.mineinabyss.keepup.downloads.github.GithubDownload
import com.mineinabyss.keepup.downloads.http.HttpDownloader
import com.mineinabyss.keepup.downloads.rclone.RcloneDownloader
import com.mineinabyss.keepup.similarfiles.SimilarFileChecker
import com.mineinabyss.keepup.type_checker.FileTypeChecker
import io.ktor.client.*
import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class DownloadParser(
    val failAllDownloads: Boolean,
    val client: HttpClient,
    val githubConfig: GithubConfig,
    val similarFileChecker: SimilarFileChecker?,
) {
    val httpRegex = "^https?://.*".toRegex()

    /**
     * Downloads a file from a [source] definition into a [targetDir]
     *
     * @param source Takes form of an https url, rclone remote, or `.` to ignore
     */
    suspend fun download(
        source: DownloadSource,
        targetDir: Path,
    ): List<DownloadResult> {
        if (failAllDownloads) {
            delay(Random.nextLong(500, 2000).milliseconds)
            return listOf(DownloadResult.Failure("Testing flag enabled", source.keyInConfig))
        }

        val downloader: Downloader = when {
            source.query == "." -> return emptyList()
            source.query.startsWith("github:") -> GithubDownload(
                client = client,
                config = githubConfig,
                artifact = GithubArtifact.from(source),
                targetDir = targetDir,
            )

            source.query.matches(httpRegex) -> HttpDownloader(client, source, targetDir)
            else -> RcloneDownloader(source, targetDir)
        }

        val results = runCatching {
            downloader.download()
        }.getOrElse { error ->
            val message = error.stackTraceToString()

            listOf(
                DownloadResult.Failure(
                    message = "Program errored,\n$message",
                    keyInConfig = source.keyInConfig,
                )
            )
        }

        return similarFileChecker?.filterSimilarFiles(results)
            // Check if the downloaded file matches the expected file type
            ?.map {
                val expectedFileType = source.expectedType
                if (it !is DownloadResult.HasFiles || expectedFileType == null) return@map it
                val type = FileTypeChecker.getType(it.file)
                if (type == expectedFileType) it
                else DownloadResult.Failure(
                    "Downloaded file type didn't match expected, got $type, expected $expectedFileType",
                    it.keyInConfig
                )
            }
            ?: results
    }
}
