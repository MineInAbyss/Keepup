package com.mineinabyss.keepup.downloads.rclone

import com.lordcodes.turtle.shellRun
import java.nio.file.Path
import kotlin.io.path.div

object Rclone {
    val rclone = "rclone"

    fun sync(source: String, targetDir: Path): Path {
        shellRun(rclone, listOf("sync", source, targetDir.toString()))
        // TODO support multiple item downloads via rclone
        val name = source.substringAfterLast("/")
        return targetDir / name
    }
}

