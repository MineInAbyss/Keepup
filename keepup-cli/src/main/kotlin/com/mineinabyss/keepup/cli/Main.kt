@file:Suppress("MemberVisibilityCanBePrivate")

package com.mineinabyss.keepup.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

class KeepupCommand : CliktCommand(allowMultipleSubcommands = true) {
    override fun run() {
    }
}

fun main(args: Array<String>) = KeepupCommand().subcommands(
    PluginsCommand(),
    ConfigCommand(),
    TemplateCommand()
).main(args)
