package com.mineinabyss.keepup

import com.mineinabyss.jsonschema.dsl.SchemaType
import com.mineinabyss.jsonschema.dsl.jsonSchema
import com.mineinabyss.jsonschema.generator.KotlinxSerializationJsonSchemaGenerator
import com.mineinabyss.keepup.config_sync.ConfigDefinition
import com.mineinabyss.keepup.config_sync.CopyPath
import com.mineinabyss.keepup.config_sync.Inventory
import kotlinx.serialization.ContextualSerializer

fun main() {
    jsonSchema {
        val generator = KotlinxSerializationJsonSchemaGenerator()
        provideDefinitions {
            generator.applyClassDescriptor(it)
        }
        definition<CopyPath> {
            anyOf({ type = SchemaType.STRING }, { generator.applyClassDescriptor(CopyPath.generatedSerializer().descriptor) })
        }
        definition<ConfigDefinition> {
            generator.applyClassDescriptor(it)
            property("variables") {
                type = SchemaType.OBJECT
                description = "Variables to be passed into pebble templates. (ex. someValue: 100)."
            }
        }
        definition(ContextualSerializer(Any::class).descriptor) {
            type = SchemaType.OBJECT
        }
        rootProperty {
            ref = definition<Inventory>()
        }
    }.toString().let(::println)
}