package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

@ConsistentCopyVisibility
data class ResolvedProtobufSchema
private constructor(
    val identity: QualifiedSchemaName,
    val declaration: ResolvedProtobufDeclaration,
    val dependencies: List<QualifiedSchemaName>,
) {
    companion object {
        fun of(declaration: ResolvedProtobufDeclaration, packageOf: QualifiedSchemaName): ResolvedProtobufSchema {
            val identity = packageOf.withTypeName(declaration.name)

            return ResolvedProtobufSchema(
                identity,
                declaration,
                declaration.referencedSchemas().filter { it != identity }.distinct().sortedBy { it.fullyQualifiedName },
            )
        }
    }
}

private fun ResolvedProtobufDeclaration.referencedSchemas(): List<QualifiedSchemaName> =
    when (this) {
        is ResolvedProtobufMessage -> messageReferences()
        is ResolvedProtobufEnum -> emptyList()
    }

private fun ResolvedProtobufMessage.messageReferences(): List<QualifiedSchemaName> =
    fields.flatMap { field ->
        when (field) {
            is ResolvedProtobufField.Scalar -> emptyList()

            is ResolvedProtobufField.Reference -> listOf(field.reference.target)

            is ResolvedProtobufField.Map -> listOfNotNull(field.value.referencedSchema())
        }
    } + oneofs.flatMap { oneof -> oneof.fields.mapNotNull { it.type.referencedSchema() } }

private fun ResolvedProtobufValueType.referencedSchema(): QualifiedSchemaName? =
    (this as? ResolvedProtobufValueType.Reference)?.reference?.target
