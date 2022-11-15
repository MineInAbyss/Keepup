import commands.Wget
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.listDirectoryEntries

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
