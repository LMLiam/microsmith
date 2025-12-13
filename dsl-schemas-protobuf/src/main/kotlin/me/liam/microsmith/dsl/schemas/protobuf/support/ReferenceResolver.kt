package me.liam.microsmith.dsl.schemas.protobuf.support

import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

fun getReferencePath(
    currentSegments: List<String>,
    target: String
): List<String> =
    when {
        // absolute or unqualified
        !target.startsWith(".") ->
            if ('.' in target) {
                target.split('.')
            } else {
                currentSegments + target
            }

        // relative: count leading dots, drop that many segments
        else -> {
            val upCount = target.takeWhile { it == '.' }.length
            val remaining = target.drop(upCount)
            currentSegments.dropLast(upCount.coerceAtMost(currentSegments.size)) + remaining.split('.')
        }
    }

fun resolveReferences(schemas: Set<ProtobufSchema>): Set<ProtobufSchema> {
    val messages = schemas.associateBy { it.name }
    val errors = mutableListOf<String>()

    fun Reference.resolve(context: String) {
        val target = messages[name]?.schema
        if (target == null) {
            errors += "Unresolved reference '$name' in $context"
        } else {
            type = target
        }
    }

    messages.values
        .map { it.schema }
        .filterIsInstance<Message>()
        .forEach { schema ->
            schema.fields
                .filterIsInstance<ReferenceField>()
                .forEach { field ->
                    field.reference.resolve("message ${schema.name} field '${field.name}'")
                }

            schema.fields
                .filterIsInstance<MapField>()
                .forEach { field ->
                    (field.type.value as? Reference)
                        ?.resolve("message ${schema.name} map field '${field.name}' value")
                }

            schema.oneofs
                .flatMap { it.fields }
                .forEach { field ->
                    (field.fieldType as? Reference)
                        ?.resolve("message ${schema.name} oneof '${field.name}'")
                }
        }

    if (errors.isNotEmpty()) {
        error(
            buildString {
                appendLine("Unresolved references:")
                errors.forEach { appendLine("- $it") }
            }.trimEnd()
        )
    }

    return schemas
}
