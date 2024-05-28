package com.mineinabyss.keepup.config.templating

import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter

class Templater {
    val pebble = PebbleEngine.Builder().autoEscaping(false).build()

    fun template(input: String, variables: Map<String, Any?>): Result<String> {
        val writer = StringWriter()
        return runCatching {
            pebble.getLiteralTemplate(input).evaluate(writer, variables)
            writer.toString()
        }
    }
}
