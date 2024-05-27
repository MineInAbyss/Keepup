package com.mineinabyss.keepup.config

import com.mineinabyss.keepup.helpers.InnerSerializer
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

@Serializable(with = Inventory.Serializer::class)
class Inventory(
    val configs: Map<String, ConfigDefinition>,
) {
    fun getOrCreateConfigs(names: Iterable<String>): List<ConfigDefinition> {
        val global = configs["global"]
        return listOfNotNull(global) + names.map {
            configs[it] ?: ConfigDefinition(
                copyPaths = listOf(it),
            )
        }
    }

    object Serializer : InnerSerializer<Map<String, ConfigDefinition>, Inventory>(
        "Inventory",
        MapSerializer(String.serializer(), ConfigDefinition.serializer()),
        { Inventory(it) },
        { it.configs }
    )
}

@Serializable
data class FileConfig(
    val deleteUntracked: Boolean = false,
    val keep: List<String> = emptyList()
)

@Serializable
data class ConfigDefinition(
    val copyPaths: List<String> = emptyList(),
    val files: Map<String, FileConfig> = mapOf(),
    val include: List<String> = listOf(),
    @Serializable(with = VariablesSerializer::class)
    val variables: Map<String, @Contextual Any?> = mapOf(),
) {
    companion object {
        fun reduce(configs: List<ConfigDefinition>) =
            configs.reduce { acc, config ->
                acc.copy(
                    copyPaths = acc.copyPaths + config.copyPaths,
                    files = acc.files + config.files,
                    variables = mergeVariables(acc.variables, config.variables)
                )
            }

        fun mergeVariables(
            acc: Map<*, *>,
            vars: Map<*, *>
        ): Map<String, Any?> {
            val merge = acc.toMutableMap()
            vars.forEach { (key, value) ->
                val existing = acc[key]
                merge[key] = when {
                    value is Map<*, *> && existing is Map<*, *> -> {
                        mergeVariables(existing, value)
                    }

                    value is List<*> && existing is List<*> -> {
                        (existing + value).distinct()
                    }

                    else -> value
                }
            }
            return merge as Map<String, Any?>
        }
    }
}


fun main() {
    println(
        ConfigDefinition.mergeVariables(
            mapOf("env" to mapOf("a" to 1)),
            mapOf("env" to mapOf("b" to 2))
        )
    )
}
