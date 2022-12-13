package commands

import com.lordcodes.turtle.ShellRunException
import com.lordcodes.turtle.shellRun
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.nio.file.Path

object Rclone {
    val rclone = "rclone"
    private val json = Json { ignoreUnknownKeys = true }

    fun sync(source: String, target: Path): List<DownloadedItem> {
        return runCatching {
            shellRun(rclone, listOf("sync", source, target.toString()))
            //TODO this could lead to problems if items change between list and sync, can we do it in one command?
            json.decodeFromString(
                ListSerializer(DownloadedItem.serializer()),
                shellRun(rclone, listOf("lsjson", "--recursive", "--files-only", source))
            )
        }
            .onSuccess { downloads -> println("Synced $source:\n${downloads.joinToString("\n") { it.relativePath }.prependIndent("  - ")}") }
            .onFailure {
                if (it is ShellRunException) {
                    println("Error getting $source\n${it.errorText.prependIndent("  ")}")
                } else it.printStackTrace()
            }
            .getOrDefault(emptyList())
    }
}

