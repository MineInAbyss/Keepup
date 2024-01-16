package commands

import com.lordcodes.turtle.ShellRunException
import com.lordcodes.turtle.shellRun
import kotlinx.serialization.json.Json
import java.nio.file.Path

object Rclone {
    val rclone = "rclone"
    private val json = Json { ignoreUnknownKeys = true }

    fun sync(source: String, targetDir: Path): DownloadedItem? {
        return runCatching {
            shellRun(rclone, listOf("sync", source, targetDir.toString()))
        }
            .map {
                val name = source.substringAfterLast("/")
                DownloadedItem("$targetDir/$name", name)
            }
            .onSuccess { println("Downloaded $source") }
            .onFailure {
                if (it is ShellRunException) System.err.println(
                    "Error downloading $source\n${
                        it.message?.prependIndent(
                            "  "
                        )
                    }"
                )
            }
            .getOrNull()
    }
}

