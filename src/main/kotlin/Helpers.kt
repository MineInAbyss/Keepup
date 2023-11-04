import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import commands.DownloadedItem
import commands.Rclone
import commands.Wget
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.*

/** Convers a HOCON [input] stream into JSON */
fun renderHocon(input: InputStream): ByteArrayInputStream {
    val config = ConfigFactory.parseReader(input.reader()).resolve()
    // Convert config to json
    return config.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))
        .byteInputStream()
}

/** Removes any symlinks contained in [path] */
fun clearSymlinks(path: Path) {
    // For each file in the directory
    path.listDirectoryEntries().forEach {
        if (!it.isDirectory() && it.isSymbolicLink()) it.deleteIfExists()
    }
}

class DownloadsContext(
    val targetDir: Path,
    val forceLatest: Boolean,
)

/**
 * Downloads a file from a [source] definition into a [targetDir]
 *
 * @param source Takes form of an https url, rclone remote, or `.` to ignore
 */
context(Keepup)
fun download(source: String, targetDir: Path): List<DownloadedItem> = when {
    source == "." -> emptyList()
    source.startsWith("github:") -> GithubDownload.from(source).download(targetDir, forceLatest)
    source.matches("^https?://.*".toRegex()) -> listOfNotNull(Wget(source, targetDir))
    else -> listOfNotNull(Rclone.sync(source, targetDir))
}

/** Fold leaf Strings of [map] into a list of Strings */
fun getLeafStrings(map: Map<String, Any?>, acc: MutableMap<String, String> = mutableMapOf()): Map<String, String> {
    map.entries.forEach { (key, value) ->
        when (value) {
            is String -> acc[key] = value
            is Map<*, *> -> getLeafStrings(value as Map<String, Any?>, acc)
        }
    }
    return acc
}

/** Creates a symlink for a downloaded [item] to a [dest] folder */
fun linkToDest(dest: Path, isolatedPath: Path, item: DownloadedItem) {
    (dest / item.name).createSymbolicLinkPointingTo(
        (isolatedPath / item.relativePath).relativeTo(dest.absolute())
    )
}

/** Removes everything between the first digit and ext of the file */
fun String.removeVersion() = "${takeWhile { !it.isDigit() }}.${takeLastWhile { it != '.' }}"

/** Checks if two strings are similar with their versions removed */
fun similar(a: String, b: String): Boolean = a.removeVersion() == b.removeVersion()
