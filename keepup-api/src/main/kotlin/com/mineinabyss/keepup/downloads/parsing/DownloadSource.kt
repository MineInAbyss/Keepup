package com.mineinabyss.keepup.downloads.parsing

import com.mineinabyss.keepup.type_checker.FileType

data class DownloadSource(
    val keyInConfig: String,
    val query: String,
    val expectedType: FileType? = guessFileType(query),
) {
    companion object {
        fun guessFileType(query: String): FileType? {
            return when {
                query.endsWith(".zip") || query.endsWith(".jar") || query.endsWith(".tar.gz") -> FileType.Archive
                query.endsWith(".html") -> FileType.HTML
                else -> null
            }
        }
    }
}
