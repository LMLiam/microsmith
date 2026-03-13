package io.github.lmliam.microsmith.gen.schemas

import io.github.lmliam.microsmith.dsl.schemas.core.Schema
import java.util.ServiceLoader
import kotlin.reflect.KClass

/**
 * Resolves schema emitters by concrete schema class and rejects ambiguous registrations.
 */
internal class SchemaEmitterRegistry(
    emitters: List<SchemaEmitter<*>> = loadSchemaEmitters(),
) {
    private val emittersByType: Map<KClass<out Schema>, SchemaEmitter<*>> = indexEmitters(emitters)

    fun resolve(schema: Schema): SchemaEmitter<Schema> = emittersByType[schema::class]
        ?.cast()
        ?: error("No emitter found for schema type: ${schema::class}")

    private fun indexEmitters(emitters: List<SchemaEmitter<*>>): Map<KClass<out Schema>, SchemaEmitter<*>> {
        val duplicates = emitters.groupBy(SchemaEmitter<*>::type).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val types = duplicates.keys.map(::formatType).sorted().joinToString(", ")
            "Duplicate schema emitters registered for schema types: $types"
        }

        return emitters.associateBy(SchemaEmitter<*>::type)
    }

    @Suppress("UNCHECKED_CAST")
    private fun SchemaEmitter<*>.cast(): SchemaEmitter<Schema> = this as SchemaEmitter<Schema>

    private fun formatType(type: KClass<out Schema>): String = type.qualifiedName ?: type.toString()
}

private fun loadSchemaEmitters(): List<SchemaEmitter<*>> =
    ServiceLoader.load(SchemaEmitter::class.java).iterator().asSequence().toList()
