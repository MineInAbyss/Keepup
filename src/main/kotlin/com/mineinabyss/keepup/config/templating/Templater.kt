package com.mineinabyss.keepup.config.templating

import io.pebbletemplates.pebble.PebbleEngine
import java.io.StringWriter

class Templater {
    val pebble = PebbleEngine.Builder().build()

    fun template(input: String, variables: Map<String, Any?>): String {
        val writer = StringWriter()
        pebble.getLiteralTemplate(input).evaluate(writer, variables)
        return writer.toString()
    }
}
