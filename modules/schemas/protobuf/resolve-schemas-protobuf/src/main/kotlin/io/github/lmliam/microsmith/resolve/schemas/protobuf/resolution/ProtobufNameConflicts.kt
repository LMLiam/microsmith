package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal fun findNameConflicts(schemas: List<ProtobufSchema>): List<ProtobufResolutionIssue> =
    schemas.sortedBy(ProtobufSchema::name).flatMap(::nameConflictsOf)

private fun nameConflictsOf(schema: ProtobufSchema): List<ProtobufResolutionIssue> =
    QualifiedSchemaName.parse(schema.name)
        .fold(
            ifLeft = { emptyList() },
            ifRight = { identity ->
                when (val declaration = schema.schema) {
                    is Message -> fieldJsonNameCollisions(identity.fullyQualifiedName, declaration)

                    is Enum -> enumValueNameCollisions(identity, declaration)

                    else -> emptyList()
                }
            },
        )

private fun fieldJsonNameCollisions(
    schemaName: String,
    message: Message,
): List<ProtobufResolutionIssue.FieldJsonNameCollision> =
    (message.fields + message.oneofs.flatMap(Oneof::fields))
        .map(Field::name)
        .filter(::isIdentifier)
        .distinct()
        .groupBy(::defaultJsonName)
        .filterValues { it.size > 1 }
        .map { (jsonName, fieldNames) ->
            ProtobufResolutionIssue.FieldJsonNameCollision(schemaName, jsonName, fieldNames)
        }

private fun defaultJsonName(fieldName: String): String = fieldName.underscoresToCamelCase(capitalizeFirst = false)

private fun enumValueNameCollisions(
    identity: QualifiedSchemaName,
    enum: Enum,
): List<ProtobufResolutionIssue.EnumValueNameCollision> =
    enum.values
        .map { it.name }
        .filter(::isIdentifier)
        .distinct()
        .groupBy { enumValueKey(identity.typeName.value, it) }
        .filterValues { it.size > 1 }
        .map { (_, valueNames) ->
            ProtobufResolutionIssue.EnumValueNameCollision(identity.fullyQualifiedName, valueNames)
        }

private fun enumValueKey(enumName: String, valueName: String): String =
    withoutEnumPrefix(enumName, valueName).split('_').joinToString("") {
        it.lowercase().replaceFirstChar(Char::uppercaseChar)
    }

private fun withoutEnumPrefix(enumName: String, valueName: String): String {
    val prefix = enumName.filter { it != '_' }.lowercase()
    val prefixLetters = valueName.withIndex().filter { it.value != '_' }.take(prefix.length)
    val startsWithPrefix =
        prefixLetters.size == prefix.length && prefixLetters.joinToString("") { it.value.lowercase() } == prefix

    if (!startsWithPrefix) return valueName

    val remainder = valueName.substring(prefixLetters.lastOrNull()?.let { it.index + 1 } ?: 0).trimStart('_')

    return remainder.ifEmpty { valueName }
}
