package com.mineinabyss.keepup.downloads

import java.nio.file.Path

sealed interface DownloadResult {
    val keyInConfig: String

    sealed interface HasFiles : DownloadResult {
        val file: Path
    }

    data class SkippedBecauseSimilar(
        override val keyInConfig: String,
        val similarTo: String,
    ) : DownloadResult

    data class SkippedBecauseCached(
        override val file: Path,
        override val keyInConfig: String,
    ) : HasFiles

    data class Downloaded(
        override val file: Path,
        override val keyInConfig: String,
        val overrideInfoMsg: String? = null,
    ) : HasFiles

    data class Failure(
        val message: String,
        override val keyInConfig: String,
    ) : DownloadResult
}
