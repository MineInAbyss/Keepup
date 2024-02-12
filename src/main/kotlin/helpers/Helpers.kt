package helpers

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import downloading.DownloadResult
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
fun linkToDest(dest: Path, source: DownloadResult.HasFiles) {
    val file = source.file
    (dest / file.name).createSymbolicLinkPointingTo((file).relativeTo(dest.absolute()))
}
