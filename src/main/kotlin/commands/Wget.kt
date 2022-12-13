package commands

import com.lordcodes.turtle.ShellRunException
import com.lordcodes.turtle.shellRun
import java.nio.file.Path

object Wget {
    operator fun invoke(url: String, target: Path): DownloadedItem? {
        val result = runCatching {
            shellRun("wget", listOf("--no-clobber", url, "-P", target.toString()))
        }
        return result.map {
            val name = url.substringAfterLast("/")
            DownloadedItem("$target/$name", name)
        }
            .onSuccess { println("Synced $url") }
            .onFailure { if (it is ShellRunException) println("Error getting $url\n${it.message?.prependIndent("  ")}") }
            .getOrNull()
    }
}
