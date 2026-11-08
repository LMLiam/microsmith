package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.raise
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ValueType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnumValue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufOneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufSchema
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufValueType
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.ProtobufDeclarationValidator

internal class ProtobufDeclarationResolver(private val referenceResolver: ProtobufReferenceResolver) {
    private val validator = ProtobufDeclarationValidator()

    fun resolveMessage(
        schema: ProtobufSchema,
        message: Message,
    ): EitherNel<ProtobufResolutionIssue, ResolvedProtobufSchema> = either {
        val identity = identityOf(schema).bind()
        val declaration =
            zipOrAccumulate(
                { checkDeclaration(identity, message) },
                { resolveFields(identity, message) },
                { resolveOneofs(identity, message) },
            ) { _, fields, oneofs ->
                ResolvedProtobufMessage(message.name, fields, oneofs, message.reserved)
            }

        schemaFor(identity, declaration)
    }

    fun resolveEnum(
        schema: ProtobufSchema,
        enumDeclaration: Enum,
    ): EitherNel<ProtobufResolutionIssue, ResolvedProtobufSchema> = either {
        val identity = identityOf(schema).bind()
        val declaration =
            zipOrAccumulate(
                { checkDeclaration(identity, enumDeclaration) },
                { enumDeclaration.values.map { ResolvedProtobufEnumValue(it.name, it.index) } },
            ) { _, values ->
                ResolvedProtobufEnum(enumDeclaration.name, values, enumDeclaration.reserved)
            }

        schemaFor(identity, declaration)
    }

    private fun identityOf(schema: ProtobufSchema): EitherNel<ProtobufResolutionIssue, QualifiedSchemaName> =
        QualifiedSchemaName.parse(schema.name).mapLeft { nonEmptyListOf<ProtobufResolutionIssue>(it) }

    context(_: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun checkDeclaration(
        identity: QualifiedSchemaName,
        declaration: Type,
    ) {
        val nameMismatch =
            ProtobufResolutionIssue.SchemaDeclarationNameMismatch(identity.fullyQualifiedName, declaration.name)
                .takeIf { identity.typeName != declaration.name }

        (validator.validate(identity.fullyQualifiedName, declaration) + listOfNotNull(nameMismatch)).mapOrAccumulate {
            raise(it)
        }
    }

    context(_: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveFields(
        identity: QualifiedSchemaName,
        message: Message,
    ): List<ResolvedProtobufField> =
        message.fields.mapOrAccumulate { field ->
            when (field) {
                is ScalarField ->
                    ResolvedProtobufField.Scalar(
                        field.name,
                        field.index,
                        field.primitive,
                        field.cardinality,
                    )

                is ReferenceField ->
                    ResolvedProtobufField.Reference(
                        field.name,
                        field.index,
                        referenceResolver
                            .resolve(
                                identity,
                                field.reference,
                                ProtobufResolutionIssue.ReferenceLocation.Field(field.name),
                            )
                            .bind(),
                        field.cardinality,
                    )

                is MapField ->
                    ResolvedProtobufField.Map(
                        field.name,
                        field.index,
                        field.type.key,
                        resolveValueType(
                            identity,
                            field.type.value,
                            ProtobufResolutionIssue.ReferenceLocation.MapValue(field.name),
                        ),
                    )
            }
        }

    context(_: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveOneofs(
        identity: QualifiedSchemaName,
        message: Message,
    ): List<ResolvedProtobufOneof> =
        message.oneofs.mapOrAccumulate { oneof ->
            ResolvedProtobufOneof(
                oneof.name,
                fields =
                    oneof.fields.mapOrAccumulate { field ->
                        ResolvedProtobufOneof.Field(
                            field.name,
                            field.index,
                            resolveValueType(
                                identity,
                                field.fieldType,
                                ProtobufResolutionIssue.ReferenceLocation.OneofField(oneof.name, field.name),
                            ),
                        )
                    },
            )
        }

    context(_: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveValueType(
        identity: QualifiedSchemaName,
        value: ValueType,
        location: ProtobufResolutionIssue.ReferenceLocation,
    ): ResolvedProtobufValueType =
        when (value) {
            is PrimitiveType -> ResolvedProtobufValueType.Primitive(value)

            is Reference ->
                ResolvedProtobufValueType.Reference(referenceResolver.resolve(identity, value, location).bind())
        }

    private fun schemaFor(
        identity: QualifiedSchemaName,
        declaration: ResolvedProtobufDeclaration,
    ): ResolvedProtobufSchema =
        ResolvedProtobufSchema(
            identity,
            declaration,
            dependencies =
                declaration.referencedSchemas().filter { it != identity }.distinct().sortedBy { it.fullyQualifiedName },
        )
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
