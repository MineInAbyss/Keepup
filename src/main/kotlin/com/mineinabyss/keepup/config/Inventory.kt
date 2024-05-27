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
                copyPaths = listOf("$it/sync"),
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
                    variables = acc.variables + config.variables
                )
            }
    }
}
