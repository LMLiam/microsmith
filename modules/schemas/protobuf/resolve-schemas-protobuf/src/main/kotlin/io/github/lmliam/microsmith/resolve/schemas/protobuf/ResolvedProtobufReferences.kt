package io.github.lmliam.microsmith.resolve.schemas.protobuf

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

internal fun referencesOf(
    fields: List<ResolvedProtobufField>,
    oneofs: List<ResolvedProtobufOneof>,
): List<LocatedReference> =
    fields.flatMap { field ->
        when (field) {
            is ResolvedProtobufField.Scalar -> emptyList()

            is ResolvedProtobufField.Reference ->
                listOf(
                    LocatedReference(ProtobufResolutionIssue.ReferenceLocation.Field(field.name.value), field.reference)
                )

            is ResolvedProtobufField.Map ->
                referenceOf(field.value, ProtobufResolutionIssue.ReferenceLocation.MapValue(field.name.value))
        }
    } +
        oneofs.flatMap { oneof ->
            oneof.fields.toList().flatMap { field ->
                referenceOf(
                    field.type,
                    ProtobufResolutionIssue.ReferenceLocation.OneofField(oneof.name.value, field.name.value),
                )
            }
        }

internal fun dependenciesOf(
    identity: QualifiedSchemaName,
    references: List<LocatedReference>,
): List<QualifiedSchemaName> =
    references.map { it.reference.target }.filter { it != identity }.distinct().sortedBy { it.fullyQualifiedName }

private fun referenceOf(
    type: ResolvedProtobufValueType,
    location: ProtobufResolutionIssue.ReferenceLocation,
): List<LocatedReference> =
    when (type) {
        is ResolvedProtobufValueType.Primitive -> emptyList()
        is ResolvedProtobufValueType.Reference -> listOf(LocatedReference(location, type.reference))
    }
