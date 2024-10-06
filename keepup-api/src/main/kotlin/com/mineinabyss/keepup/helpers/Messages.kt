package com.mineinabyss.keepup.helpers

import com.github.ajalt.mordant.rendering.TextColors.*

object MSG {
    val info = buildString {
        append(brightWhite("["))
        append(brightRed("K"))
        append(brightYellow("e"))
        append(brightGreen("e"))
        append(brightCyan("p"))
        append(brightBlue("u"))
        append(brightMagenta("p"))
        append(brightWhite("]"))
    }
    val download = brightBlue("[Downloaded]")
    val failure = brightRed("[Failure]   ")
    val cached = brightGreen("[Use Cached]")
    val github = gray("[Github]    ")
    val rclone = brightBlue("[Rclone]    ")
    val skipped = brightYellow("[Ignoring]  ")

    val warn = yellow("[Warn]")
    val error = red("[Error]")
    val delete = red("[Delete]")
    val copy = brightBlue("[Copy]")
    val template = brightBlue("[Template]")
}
