package downloading.github

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextColors.gray
import commands.CachedCommand
import config.GithubConfig
import downloading.DownloadResult
import downloading.Downloader
import downloading.HttpDownload
import downloading.Source
import helpers.GithubReleaseOverride
import helpers.MSG
import io.ktor.client.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import t
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Ex url "github:MineInAbyss/Idofront:v0.20.6:*.jar"
 */
class GithubDownload(
    val client: HttpClient,
    val config: GithubConfig,
    val artifact: GithubArtifact,
    val targetDir: Path,
) : Downloader {

    override suspend fun download(): List<DownloadResult> {
        val version = when (config.overrideGithubRelease) {
            GithubReleaseOverride.LATEST_RELEASE -> "latest-release"
            GithubReleaseOverride.LATEST -> "latest"
            else -> artifact.releaseVersion
        }
        val releaseURL = if (version == "latest") "latest" else "tags/${artifact.releaseVersion}"

        //TODO convert to ktor
        val commandResult = CachedCommand(
            buildString {
                append("curl -s ")
                if (config.githubAuthToken != null)
                    append("-H \"Authorization: token ${config.githubAuthToken}\" ")
                if (config.overrideGithubRelease == GithubReleaseOverride.LATEST)
                    append("https://api.github.com/repos/${artifact.repo}/releases | jq 'map(select(.draft == false)) | sort_by(.published_at) | last'")
                else
                    append("https://api.github.com/repos/${artifact.repo}/releases/$releaseURL")
                append(" | grep 'browser_download_url'")
            },
            targetDir / "response-${artifact.repo.replace("/", "-")}-$version",
            expiration = config.cacheExpirationTime.takeIf { version == "latest" }
        ).getFromCacheOrEval()

        val downloadURLs = commandResult
            .result
            .split("\n")
            .map { it.trim().removePrefix("\"browser_download_url\": \"").trim('"') }
            .filter { it.contains(artifact.calculatedRegex) }

        val fullName = TextColors.yellow("github:${artifact.repo}:$version:${artifact.regex}")
        if (!commandResult.wasCached) {
            t.println(gray("${MSG.github} Got artifact URLs for $fullName"))
        }

        return coroutineScope {
            downloadURLs.map { url ->
                async { HttpDownload(client, Source(artifact.source.keyInConfig, url), targetDir).download() }
            }.awaitAll().flatten()
        }
    }
}
