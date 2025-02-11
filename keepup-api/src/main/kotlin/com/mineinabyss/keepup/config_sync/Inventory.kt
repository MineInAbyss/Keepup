package com.mineinabyss.keepup.config_sync

import com.charleskorn.kaml.*
import com.mineinabyss.keepup.helpers.InnerSerializer
import kotlinx.serialization.*
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.io.InputStream

@Serializable(with = Inventory.Serializer::class)
class Inventory(
    val configs: Map<String, ConfigDefinition>,
) {
    fun getOrCreateConfigs(host: String): List<ConfigDefinition> {
        val global = configs["global"]
        val includes = getDeepIncludes(names = listOf(host)).distinct().reversed()
        return listOfNotNull(global) + includes.map {
            configs[it] ?: ConfigDefinition(copyPaths = listOf(CopyPath(source = it)))
        }
    }

    tailrec fun getDeepIncludes(acc: MutableList<String> = mutableListOf(), names: List<String>): List<String> {
        if (names.isEmpty()) return acc
        val namesSet = names.toSet()
        val nonCyclicNames = names - acc.filter { it in namesSet }.toSet()
        acc.addAll(nonCyclicNames)
        val newNames = nonCyclicNames.flatMap { configs[it]?.include?.reversed() ?: emptyList() }
        return getDeepIncludes(acc, newNames)
    }

    object Serializer : InnerSerializer<Map<String, ConfigDefinition>, Inventory>(
        "Inventory",
        MapSerializer(String.serializer(), ConfigDefinition.serializer()),
        { Inventory(it) },
        { it.configs }
    )

    companion object {
        fun from(inputStream: InputStream) = Yaml.default.decodeFromStream<Inventory>(inputStream)
    }
}

@Serializable
data class FileConfig(
    val deleteUntracked: Boolean = false,
    val keep: List<String> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = CopyPath.Serializer::class)
@KeepGeneratedSerializer
data class CopyPath(
    /** Offset from target path to copy to (i.e. target / dest / *files in source*). */
    val dest: String = ".",
    val source: String,
) {
    object InlineSeriailzer : KSerializer<CopyPath> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CopyPath", PrimitiveKind.STRING)
        override fun serialize(encoder: Encoder, value: CopyPath) = encoder.encodeString(value.source)
        override fun deserialize(decoder: Decoder): CopyPath = CopyPath(source = decoder.decodeString())
    }

    object Serializer : YamlContentPolymorphicSerializer<CopyPath>(CopyPath::class) {
        override fun selectDeserializer(node: YamlNode): DeserializationStrategy<CopyPath> {
            return if (node is YamlScalar) InlineSeriailzer
            else generatedSerializer()
        }
    }
}

@Serializable
data class ConfigDefinition(
    val copyPaths: List<CopyPath> = emptyList(),
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
            vars: Map<*, *>,
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
