package downloading

import SimilarFileChecker
import com.mineinabyss.keepup.commands.plugins
import config.GithubConfig
import downloading.github.GithubArtifact
import downloading.github.GithubDownload
import io.ktor.client.*
import kotlinx.coroutines.delay
import java.nio.file.Path
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

class DownloadParser(
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
    suspend fun download(source: Source, targetDir: Path): List<DownloadResult> {
        if (plugins.failAllDownloads) {
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

            source.query.matches(httpRegex) -> HttpDownload(client, source, targetDir)
            else -> RcloneDownload(source, targetDir)
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

        return similarFileChecker?.filterSimilarFiles(results) ?: results
    }
}
