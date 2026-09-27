package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ValueType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReference
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufValueType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.ProtobufDeclarationValidator

internal class ProtobufDeclarationResolver(private val referenceResolver: ProtobufReferenceResolver) {
    private val validator = ProtobufDeclarationValidator()

    fun resolve(schema: ProtobufSchema): EitherNel<ProtobufResolutionIssue, ResolvedProtobufSchema> {
        val identity = QualifiedSchemaName.parse(schema.name)
        val issues = mutableListOf<ProtobufResolutionIssue>()
        val dependencies = mutableSetOf<QualifiedSchemaName>()
        val source = schema.schema
        issues += validator.validate(identity.fullyQualifiedName, source)
        if (identity.typeName != source.name) {
            issues += ProtobufResolutionIssue.SchemaDeclarationNameMismatch(identity.fullyQualifiedName, source.name)
        }

        val declaration = when (source) {
            is Message -> resolveMessage(identity, source, issues, dependencies)

            is Enum -> ResolvedProtobufEnum(
                source.name,
                values = source.values.map {
                    ResolvedProtobufEnumValue(it.name, it.index)
                },
                source.reserved,
            )

            else -> error("Unsupported protobuf declaration: ${source::class.qualifiedName}")
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            Either.Left(accumulatedIssues)
        } else {
            Either.Right(
                ResolvedProtobufSchema(
                    identity,
                    declaration,
                    dependencies = dependencies
                        .filter { it != identity }
                        .sortedBy { it.fullyQualifiedName },
                ),
            )
        }
    }

    private fun resolveMessage(
        identity: QualifiedSchemaName,
        message: Message,
        issues: MutableList<ProtobufResolutionIssue>,
        dependencies: MutableSet<QualifiedSchemaName>,
    ): ResolvedProtobufMessage {
        val fields = message.fields.mapNotNull { field ->
            when (field) {
                is ScalarField -> ResolvedProtobufField.Scalar(
                    field.name,
                    field.index,
                    field.primitive,
                    field.cardinality,
                )

                is ReferenceField -> resolveReference(
                    identity,
                    field.reference,
                    ProtobufResolutionIssue.ReferenceLocation.Field(field.name),
                    issues,
                    dependencies,
                )?.let { reference ->
                    ResolvedProtobufField.Reference(field.name, field.index, reference, field.cardinality)
                }

                is MapField -> resolveValueType(
                    identity,
                    field.type.value,
                    ProtobufResolutionIssue.ReferenceLocation.MapValue(field.name),
                    issues,
                    dependencies,
                )?.let { value ->
                    ResolvedProtobufField.Map(field.name, field.index, field.type.key, value)
                }
            }
        }

        val oneofs = message.oneofs.map { oneof ->
            ResolvedProtobufOneof(
                oneof.name,
                fields = oneof.fields.mapNotNull { field ->
                    resolveValueType(
                        identity,
                        field.fieldType,
                        ProtobufResolutionIssue.ReferenceLocation.OneofField(oneof.name, field.name),
                        issues,
                        dependencies,
                    )?.let { value ->
                        ResolvedProtobufOneof.Field(field.name, field.index, value)
                    }
                },
            )
        }

        return ResolvedProtobufMessage(message.name, fields, oneofs, message.reserved)
    }

    private fun resolveValueType(
        identity: QualifiedSchemaName,
        value: ValueType,
        location: ProtobufResolutionIssue.ReferenceLocation,
        issues: MutableList<ProtobufResolutionIssue>,
        dependencies: MutableSet<QualifiedSchemaName>,
    ): ResolvedProtobufValueType? = when (value) {
        is PrimitiveType -> ResolvedProtobufValueType.Primitive(value)

        is Reference -> resolveReference(identity, value, location, issues, dependencies)?.let {
            ResolvedProtobufValueType.Reference(it)
        }
    }

    private fun resolveReference(
        identity: QualifiedSchemaName,
        reference: Reference,
        location: ProtobufResolutionIssue.ReferenceLocation,
        issues: MutableList<ProtobufResolutionIssue>,
        dependencies: MutableSet<QualifiedSchemaName>,
    ): ResolvedProtobufReference? = referenceResolver.resolve(identity, reference, location).fold(
        ifLeft = { issue ->
            issues += issue
            null
        },
        ifRight = { resolved ->
            dependencies += resolved.target
            resolved
        },
    )
}
