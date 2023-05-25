package commands

import com.lordcodes.turtle.ShellRunException
import com.lordcodes.turtle.shellRun
import java.nio.file.Path

object Wget {
    operator fun invoke(url: String, targetDir: Path): DownloadedItem? {
        val result = runCatching {
            shellRun("wget", listOf("--no-clobber", url, "-P", targetDir.toString()))
        }
        return result.map {
            val name = url.substringAfterLast("/")
            DownloadedItem("$targetDir/$name", name)
        }
            .onSuccess { println("Downloaded $url") }
            .onFailure { if (it is ShellRunException) println("Error downloading $url\n${it.message?.prependIndent("  ")}") }
            .getOrNull()
    }
}
