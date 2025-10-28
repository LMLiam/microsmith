package me.liam.microsmith.dsl.schemas.protobuf.support

import jdk.internal.joptsimple.internal.Messages.message
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufMessageSchema
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField

fun getReferencePath(currentSegments: List<String>, target: String): List<String> {
    if (!target.startsWith(".")) {
        return if ('.' in target) {
            target.split(".")
        } else {
            currentSegments + target
        }
    }

    val segments = currentSegments.toMutableList()
    var remaining = target
    while (remaining.startsWith(".")) {
        if (segments.isNotEmpty()) segments.removeLast()
        remaining = remaining.removePrefix(".")
    }
    return segments + remaining.split(".")
}

fun resolveReferences(schemas: Set<ProtobufSchema>): Set<ProtobufSchema> {
    val messages = schemas
        .filterIsInstance<ProtobufMessageSchema>()
        .associateBy { it.name }

    fun Reference.resolve() {
        val target = messages[name]?.message
        checkNotNull(target) { "Unable to resolve reference: $name" }
        type = target
    }

    messages.values.forEach { schema ->
        schema.message.fields
            .filterIsInstance<ReferenceField>()
            .forEach { it.reference.resolve() }

        schema.message.oneofs
            .flatMap { it.fields }
            .mapNotNull { it.fieldType as? Reference }
            .forEach { it.resolve() }
    }

    return schemas
}