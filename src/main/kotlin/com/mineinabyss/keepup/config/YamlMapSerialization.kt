package com.mineinabyss.keepup.config

import com.charleskorn.kaml.*

fun parseMap(yamlNode: YamlMap): Map<String, Any?> = yamlNode.entries.map { (key, value) ->
    key.content to parseNode(value)
}.toMap()

fun parseNode(yamlNode: YamlNode): Any? = when (yamlNode) {
    is YamlMap -> parseMap(yamlNode)

    is YamlScalar -> yamlNode.content

    is YamlList -> yamlNode.items.map { parseNode(it) }

    is YamlNull -> null

    else -> println("Unknown type: $yamlNode")
}


