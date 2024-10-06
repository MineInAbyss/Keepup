package com.mineinabyss.keepup.helpers

import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayInputStream
import java.io.InputStream

/** Convers a HOCON [input] stream into JSON */
fun renderHocon(input: InputStream): ByteArrayInputStream {
    val config = ConfigFactory.parseReader(input.reader()).resolve()
    // Convert config to json
    return config.root().render(ConfigRenderOptions.defaults().setComments(false).setOriginComments(false))
        .byteInputStream()
}

/** Fold leaf Strings of [map] into a list of Strings */
fun getLeafStrings(map: JsonObject, acc: MutableMap<String, String> = mutableMapOf()): Map<String, String> {
    map.entries.forEach { (key, value) ->
        when (value) {
            is JsonPrimitive -> acc[key] = value.content
            is JsonObject -> getLeafStrings(value, acc)
            else -> return acc
        }
    }
    return acc
}

