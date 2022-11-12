import com.lordcodes.turtle.shellRun
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries

object Rclone {
    fun sync(source: String, target: String): String =
        shellRun("rsync", listOf("sync", source, target))
}

object Wget {
    operator fun invoke(source: String, target: String) =
        shellRun("wget", listOf(source, "-P", target))
}

fun clearSymlinks(path: Path) {
    // For each file in the directory
    path.listDirectoryEntries().forEach {
        if (!it.isDirectory() && it.isSymbolicLink()) it.deleteIfExists()
    }
}

fun getLeafStrings(map: Map<String, Any?>, acc: MutableMap<String, String> = mutableMapOf()): Map<String, String> {
    map.entries.forEach { (key, value) ->
        when (value) {
            is String -> acc[key] = value
            is Map<*, *> -> getLeafStrings(value as Map<String, Any?>, acc)
        }
    }
    return acc
}
