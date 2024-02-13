package helpers

import com.github.ajalt.mordant.rendering.TextColors.*
import downloading.DownloadResult
import t
import kotlin.io.path.name


fun DownloadResult.printToConsole() {
    val formattedKey = yellow(keyInConfig)

    when (this) {
        is DownloadResult.Failure -> {
            t.println(brightRed("${MSG.failure} $formattedKey: $message"), stderr = true)
        }

        is DownloadResult.Downloaded -> {
            t.println("${overrideInfoMsg ?: MSG.download} $formattedKey ${gray("(${file.name})")}")
        }

        is DownloadResult.SkippedBecauseCached -> {
            t.println("${MSG.cached} $formattedKey ${gray("(${file.name})")}")
        }

        is DownloadResult.SkippedBecauseSimilar -> {
            t.println("${MSG.skipped} $formattedKey ${gray("(similar to $similarTo)")}")
        }
    }
}
