package downloading.github

import com.github.ajalt.mordant.rendering.TextColors
import com.mineinabyss.keepup.commands.t
import config.GithubConfig
import downloading.DownloadResult
import downloading.Downloader
import downloading.HttpDownload
import downloading.Source
import helpers.CachedRequest
import helpers.GithubReleaseOverride
import helpers.MSG
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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
    val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class GithubRelease(
        val published_at: String,
        val assets: List<Asset>
    )

    @Serializable
    data class Asset(
        val browser_download_url: String
    )

    @Serializable
    data class GithubErrorMessage(
        val message: String
    )

    override suspend fun download(): List<DownloadResult> {
        val version = when (config.overrideGithubRelease) {
            GithubReleaseOverride.LATEST -> "latest"
            else -> artifact.releaseVersion
        }

        val response = CachedRequest(
            targetDir / "response-${artifact.repo.replace("/", "-")}-$version",
            expiration = config.cacheExpirationTime.takeIf { version == "latest" }
        ) {
            val response = client.get {
                timeout {
                    requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
                }
                if (config.overrideGithubRelease == GithubReleaseOverride.LATEST)
                    url("https://api.github.com/repos/${artifact.repo}/releases")
                else {
                    val releaseURL = if (version == "latest") "latest" else "tags/${artifact.releaseVersion}"
                    url("https://api.github.com/repos/${artifact.repo}/releases/$releaseURL")
                }
                headers {
                    if (config.githubAuthToken != null)
                        append(HttpHeaders.Authorization, "token ${config.githubAuthToken}")
                }
            }
            if (response.status != HttpStatusCode.OK) {
                return@CachedRequest Result.failure(RuntimeException("GET responded with error: ${response.status}, ${response.bodyAsText()}"))
            }

            Result.success(response.bodyAsText())
        }.getFromCacheOrEval().getOrElse {
            return listOf(DownloadResult.Failure(it.message ?: "", artifact.source.keyInConfig))
        }

        val body = response.result

        val release: GithubRelease = runCatching {
            if (config.overrideGithubRelease == GithubReleaseOverride.LATEST) {
                json.decodeFromString(ListSerializer(GithubRelease.serializer()), body)
                    .maxBy { it.published_at }
            } else json.decodeFromString(GithubRelease.serializer(), body)
        }.getOrElse {
            return listOf(
                DownloadResult.Failure(
                    "Failed to parse GitHub response:\n${it.message}",
                    artifact.source.keyInConfig
                )
            )
        }
        val downloadURLs = release.assets
            .map { it.browser_download_url }
            .filter { it.contains(artifact.calculatedRegex) }

        val fullName = TextColors.yellow(artifact.source.keyInConfig)

        if (!response.wasCached) {
            t.println(TextColors.gray("${MSG.github} $fullName GET artifact URLs"))
        }

        return coroutineScope {
            downloadURLs.map { url ->
                async { HttpDownload(client, Source(artifact.source.keyInConfig, url), targetDir).download() }
            }.awaitAll().flatten()
        }
    }
}
