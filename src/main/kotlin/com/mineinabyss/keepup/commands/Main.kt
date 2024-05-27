@file:Suppress("MemberVisibilityCanBePrivate")

package com.mineinabyss.keepup.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal


val plugins by lazy { PluginsCommand() }
val t by lazy { Terminal() }

class KeepupCommand : CliktCommand(allowMultipleSubcommands = true) {
    override fun run() {
    }
}

fun main(args: Array<String>) = KeepupCommand().subcommands(
    plugins,
    ConfigCommand(),
    TemplateCommand()
).main(args)
