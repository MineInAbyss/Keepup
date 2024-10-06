package com.mineinabyss.keepup.helpers

import com.github.ajalt.mordant.rendering.TextColors.*
import com.mineinabyss.keepup.downloads.DownloadResult
import kotlin.io.path.name


fun DownloadResult.printToConsole() {
    val formattedKey = yellow(keyInConfig)

    when (this) {
        is DownloadResult.Failure -> {
            com.mineinabyss.keepup.t.println(brightRed("${MSG.failure} $formattedKey: $message"), stderr = true)
        }

        is DownloadResult.Downloaded -> {
            com.mineinabyss.keepup.t.println("${overrideInfoMsg ?: MSG.download} $formattedKey ${gray("(${file.name})")}")
        }

        is DownloadResult.SkippedBecauseCached -> {
            com.mineinabyss.keepup.t.println("${MSG.cached} $formattedKey ${gray("(${file.name})")}")
        }

        is DownloadResult.SkippedBecauseSimilar -> {
            com.mineinabyss.keepup.t.println("${MSG.skipped} $formattedKey ${gray("(similar to $similarTo)")}")
        }
    }
}
