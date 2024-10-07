package com.mineinabyss.keepup.type_checker

import com.lordcodes.turtle.shellRun
import java.nio.file.Path
import kotlin.io.path.absolutePathString

object FileTypeChecker {
    fun getType(file: Path): FileType {
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
