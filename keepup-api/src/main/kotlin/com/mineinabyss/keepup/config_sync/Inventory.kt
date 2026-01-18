package com.mineinabyss.keepup.config_sync

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlContentPolymorphicSerializer
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.mineinabyss.jsonschema.annotations.Description
import com.mineinabyss.keepup.config_sync.templating.Templater
import com.mineinabyss.keepup.helpers.InnerSerializer
import com.mineinabyss.keepup.helpers.MSG
import com.mineinabyss.keepup.t
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
        fun from(
            templater: Templater,
            inputStream: InputStream,
            environment: Map<String, String> = System.getenv().toMap(),
        ): Inventory {
            t.println("${MSG.info} Parsing inventory file")
            val templatedText = templater.template(
                inputStream.bufferedReader().readText(),
                environment
            ).getOrElse {
                t.println("${MSG.error} Failed to template inventory file!")
                throw it
            }
            return Yaml.default.decodeFromString<Inventory>(templatedText)
        }
    }
}

@Serializable
data class FileConfig(
    @Description(
        "Whether to delete files that are no longer specified in any Keepup sources. " +
                "Keepup will look at all files copied over and delete anything in this directory that wasn't part of this list."
    )
    val deleteUntracked: Boolean = false,
    @Description("Specifies specific files or directories to NOT delete, ex. (`test.yml` will keep a single file, `someDirectory/` will keep an entire directory.)")
    val keep: List<String> = emptyList(),
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = CopyPath.Serializer::class)
@KeepGeneratedSerializer
data class CopyPath(
    /** Offset from target path to copy to (i.e. target / dest / *files in source*). */
    val dest: String = "",
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

    override fun toString() = "$source -> $dest"
}

@Serializable
data class ConfigDefinition(
    @Description("List of paths to copy, each specifies a source and destination. If only a string is specified, assumes source and dest are the same (up to relative paths.)")
    val copyPaths: List<CopyPath> = emptyList(),
    @Description("Options for destination paths, ex. deleting untracked files.")
    val files: Map<String, FileConfig> = mapOf(),
    @Description("List of other configs to include in this config, properties will be merged with this config taking precedence.")
    val include: List<String> = listOf(),
    @Serializable(with = VariablesSerializer::class)
    @Description("Variables to be passed into pebble templates. (ex. someValue: 100).")
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
