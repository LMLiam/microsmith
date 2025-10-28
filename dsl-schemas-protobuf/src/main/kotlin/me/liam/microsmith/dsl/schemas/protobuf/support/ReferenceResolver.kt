package me.liam.microsmith.dsl.schemas.protobuf.support

import me.liam.microsmith.dsl.schemas.protobuf.ProtobufEnumSchema
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufMessageSchema
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField

fun getReferencePath(currentSegments: List<String>, target: String): List<String> =
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
            currentSegments
                .dropLast(upCount.coerceAtMost(currentSegments.size)) +
                    remaining.split('.')
        }
    }

fun resolveReferences(schemas: Set<ProtobufSchema>): Set<ProtobufSchema> {
    val messages = schemas.associateBy { it.name }

    fun Reference.resolve() {
        val target = when (val m = messages[name]) {
            is ProtobufMessageSchema -> m.message
            is ProtobufEnumSchema -> m.enum
            else -> null
        }
        checkNotNull(target) { "Unable to resolve reference: $name" }
        type = target
    }

    messages.values
        .filterIsInstance<ProtobufMessageSchema>()
        .forEach { schema ->
            schema.message.fields
                .filterIsInstance<ReferenceField>()
                .forEach { it.reference.resolve() }

            schema.message.fields
                .filterIsInstance<MapField>()
                .mapNotNull { it.type.value as? Reference }
                .forEach { it.resolve() }

            schema.message.oneofs
                .flatMap { it.fields }
                .mapNotNull { it.fieldType as? Reference }
                .forEach { it.resolve() }
        }

    return schemas
}