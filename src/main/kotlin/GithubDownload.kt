import commands.CachedCommand
import commands.DownloadedItem
import commands.Wget
import helpers.GithubReleaseOverride
import java.nio.file.Path
import kotlin.io.path.div

/**
 * Ex url "github:MineInAbyss/Idofront:v0.20.6:*.jar"
 */
context(Keepup)
class GithubDownload(val repo: String, val releaseVersion: String, val artifactRegexString: String) {
    val artifactRegex = artifactRegexString.toRegex()

    companion object {
        context(Keepup)
        fun from(string: String): GithubDownload {
            val (repo, release, grep) = string.removePrefix("github:").split(":")
            return GithubDownload(repo, release, grep)
        }
    }

    fun download(targetDir: Path, forceLatest: GithubReleaseOverride): List<DownloadedItem> {
        val version = if (forceLatest == GithubReleaseOverride.LATEST_RELEASE) "latest" else releaseVersion
        val releaseURL = if (version == "latest") "latest" else "tags/$releaseVersion"
        val downloadURLs = CachedCommand(
            buildString {
                append("curl -s ")
                if (githubAuthToken != null)
                    append("-H \"Authorization: token $githubAuthToken\" ")
                if (overrideGithubRelease == GithubReleaseOverride.LATEST)
                    append("https://api.github.com/repos/$repo/releases | jq 'map(select(.draft == false)) | sort_by(.published_at) | last'")
                else
                    append("https://api.github.com/repos/$repo/releases/$releaseURL")
                append(" | grep 'browser_download_url'")
            },
            targetDir / "response-${repo.replace("/", "-")}-$version",
            expiration = cacheExpirationTime.takeIf { version == "latest" }
        ).getFromCacheOrEval().split("\n")
            .map { it.trim().removePrefix("\"browser_download_url\": \"").trim('"') }
            .filter { it.contains(artifactRegex) }

        println("Got URLs github:$repo:$version:$artifactRegexString $downloadURLs")

        return downloadURLs.mapNotNull { Wget(it, targetDir) }
    }
}
