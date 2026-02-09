package me.liam.microsmith.dsl.schemas.protobuf.support

import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapType
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

fun getReferencePath(
    currentSegments: List<String>,
    target: String
): List<String> {
    require(target.isNotBlank()) { "Reference target cannot be blank." }

    return when {
        !target.startsWith(".") ->
            if ('.' in target) {
                target.validatePathSegments("Reference target")
            } else {
                currentSegments + target
            }

        else -> {
            val upCount = target.takeWhile { it == '.' }.length
            val remaining = target.drop(upCount)
            require(remaining.isNotBlank()) { "Reference target cannot end with only dots: '$target'" }

            currentSegments.dropLast(upCount.coerceAtMost(currentSegments.size)) +
                remaining.validatePathSegments("Reference target")
        }
    }
}

private fun String.validatePathSegments(label: String): List<String> {
    val segments = split('.')
    require(segments.none { it.isBlank() }) { "$label contains empty path segments: '$this'" }
    return segments
}

fun resolveReferences(schemas: Set<ProtobufSchema>): Set<ProtobufSchema> {
    val schemasByName = schemas.associateBy { it.name }
    val errors = mutableListOf<String>()

    fun Reference.resolve(context: String): Reference {
        val target = schemasByName[name]?.schema
        if (target == null) {
            errors += "Unresolved reference '$name' in $context"
            return this
        }
        return copy(type = target)
    }

    fun Field.resolve(messageName: String): Field =
        when (this) {
            is ReferenceField -> copy(reference = reference.resolve("message $messageName field '$name'"))
            is MapField -> {
                val resolvedValue =
                    (type.value as? Reference)?.resolve("message $messageName map field '$name' value") ?: type.value
                copy(type = MapType(type.key, resolvedValue))
            }
            else -> this
        }

    fun OneofField.resolve(messageName: String, oneofName: String): OneofField {
        val resolvedFieldType =
            (fieldType as? Reference)?.resolve("message $messageName oneof '$oneofName' field '$name'") ?: fieldType
        return copy(fieldType = resolvedFieldType)
    }

    val resolvedSchemas =
        schemas.map { schema ->
            val resolvedType =
                when (val current = schema.schema) {
                    is Message ->
                        current.copy(
                            fields = current.fields.map { field -> field.resolve(current.name) },
                            oneofs =
                                current.oneofs.map { oneof ->
                                    Oneof(
                                        name = oneof.name,
                                        fields = oneof.fields.map { oneofField -> oneofField.resolve(current.name, oneof.name) }
                                    )
                                }
                        )

                    else -> current
                }

            schema.copy(schema = resolvedType)
        }.toSet()

    if (errors.isNotEmpty()) {
        error(
            buildString {
                appendLine("Unresolved references:")
                errors.forEach { appendLine("- $it") }
            }.trimEnd()
        )
    }

    return resolvedSchemas
}
