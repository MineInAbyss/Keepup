import commands.DownloadedItem
import commands.Wget
import java.nio.file.Path

/**
 * Ex url "github:MineInAbyss/Idofront:v0.20.6:*.jar"
 */
class GithubDownload(val repo: String, val releaseVersion: String, val artifactRegex: String) {
    companion object {
        fun from(string: String): GithubDownload {
            val (repo, release, grep) = string.removePrefix("github:").split(":")
            return GithubDownload(repo, release, grep)
        }

    }

    fun download(targetDir: Path, forceLatest: Boolean): List<DownloadedItem> {
        val version = if (forceLatest) "latest" else releaseVersion
        val releaseURL = if (version == "latest") "latest" else "tags/$releaseVersion"
        val formatted =
            "curl -s https://api.github.com/repos/$repo/releases/$releaseURL | grep 'browser_download_url$artifactRegex' | cut -d : -f 2,3 | tr -d \\\""
        val urls = formatted.evalBash(env = mapOf()).getOrThrow().split("\n ")
        println("Got URLs github:$repo:$version:$artifactRegex $urls")

        return urls.mapNotNull { Wget(it, targetDir) }
    }
}
