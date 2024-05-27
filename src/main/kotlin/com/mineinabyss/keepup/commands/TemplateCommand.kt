package com.mineinabyss.keepup.commands

import com.charleskorn.kaml.Yaml
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.mineinabyss.keepup.config.VariablesSerializer
import com.mineinabyss.keepup.config.templating.Templater

class TemplateCommand : CliktCommand(help = "Previews Pebble template result") {
    val input by argument(help = "Input stream to template")
    val variables by argument(help = "Yaml formatted variables to template with")

    override fun run() {
        val vars = Yaml.default.decodeFromString(VariablesSerializer, variables)
        val output = Templater().template(input, vars)
        t.println(output)
    }
}
