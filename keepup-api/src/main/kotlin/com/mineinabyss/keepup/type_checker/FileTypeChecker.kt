package com.mineinabyss.keepup.type_checker

import com.github.ajalt.mordant.rendering.TextColors
import com.lordcodes.turtle.shellRun
import com.mineinabyss.keepup.helpers.MSG
import com.mineinabyss.keepup.t
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object FileTypeChecker {
    val SYSTEM_SUPPORTS_FILE by lazy {
        runCatching {
            shellRun("command", listOf("-v", "file"))
        }.onFailure {
            t.println("${MSG.warn} ${TextColors.yellow("System does not support file command, file type checking will be disabled")}")
        }.isSuccess
    }

    fun getType(file: Path): FileType? {
        if (!SYSTEM_SUPPORTS_FILE) return null

        val result = shellRun("file", listOf("-b", file.absolutePathString()))
        return when {
            result.startsWith("Java archive data") || result.startsWith("Zip archive data") -> FileType.Archive
            result.startsWith("HTML document") -> FileType.HTML
            else -> FileType.Other(result)
        }
    }

    fun matches(file: Path, type: FileType): Boolean {
        return getType(file) == type
    }
}
