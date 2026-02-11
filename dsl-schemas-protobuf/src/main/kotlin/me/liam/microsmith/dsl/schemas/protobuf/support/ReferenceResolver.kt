package me.liam.microsmith.dsl.schemas.protobuf.support

import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapType
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.types.Message

fun getReferencePath(currentSegments: List<String>, target: String): List<String> {
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

private class ReferenceResolutionContext(
    schemas: Set<ProtobufSchema>,
) {
    private val schemasByName = schemas.associateBy { it.name }
    val errors = mutableListOf<String>()

    private fun Reference.resolve(context: String): Reference {
        val target = schemasByName[name]?.schema
        if (target == null) {
            errors += "Unresolved reference '$name' in $context"
            return this
        }
        return copy(type = target)
    }

    private fun Field.resolve(messageName: String): Field = when (this) {
        is ReferenceField -> copy(reference = reference.resolve("message $messageName field '$name'"))
        is MapField -> {
            val resolvedValue =
                (type.value as? Reference)?.resolve("message $messageName map field '$name' value") ?: type.value
            copy(type = MapType(type.key, resolvedValue))
        }
        is ScalarField -> this
        is OneofField -> this
    }

    private fun OneofField.resolve(messageName: String, oneofName: String): OneofField {
        val resolvedFieldType =
            (fieldType as? Reference)?.resolve("message $messageName oneof '$oneofName' field '$name'") ?: fieldType
        return copy(fieldType = resolvedFieldType)
    }

    private fun Message.resolveMessage(): Message = copy(
        fields = fields.map { it.resolve(name) },
        oneofs = oneofs.map { it.resolve(name) },
    )

    private fun Oneof.resolve(messageName: String): Oneof = copy(fields = fields.map { it.resolve(messageName, name) })

    fun resolve(schema: ProtobufSchema): ProtobufSchema {
        val resolvedType =
            when (val current = schema.schema) {
                is Message -> current.resolveMessage()
                else -> current
            }
        return schema.copy(schema = resolvedType)
    }

    fun failOnUnresolvedReferences() {
        if (errors.isEmpty()) {
            return
        }
        error(
            buildString {
                appendLine("Unresolved references:")
                errors.forEach { appendLine("- $it") }
            }.trimEnd(),
        )
    }
}

fun resolveReferences(schemas: Set<ProtobufSchema>): Set<ProtobufSchema> {
    val resolver = ReferenceResolutionContext(schemas)
    val resolvedSchemas = schemas.map { resolver.resolve(it) }.toSet()
    resolver.failOnUnresolvedReferences()
    return resolvedSchemas
}
