package com.mineinabyss.keepup.config

import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.yamlMap
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@OptIn(ExperimentalSerializationApi::class)
object VariablesSerializer : KSerializer<Map<String, Any?>> {
    override val descriptor: SerialDescriptor
        get() = MapSerializer(String.serializer(), ContextualSerializer(Any::class)).descriptor

    override fun deserialize(decoder: Decoder): Map<String, Any?> {
        val input = decoder.beginStructure(descriptor) as YamlInput
        return parseMap(input.node.yamlMap)
    }

    override fun serialize(encoder: Encoder, value: Map<String, Any?>) {
        TODO("Not yet implemented")
    }
}
