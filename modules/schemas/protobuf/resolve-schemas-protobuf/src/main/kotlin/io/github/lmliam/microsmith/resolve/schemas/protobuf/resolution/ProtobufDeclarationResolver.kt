package io.github.lmliam.microsmith.resolve.schemas.protobuf.resolution

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.bind
import arrow.core.raise.context.bindNel
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MessageField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ValueType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
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
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing.ParsedField
import io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing.ParsedOneof
import io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing.parseEnumDeclaration
import io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing.parseIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing.parseMessageDeclaration

internal class ProtobufDeclarationResolver(private val referenceResolver: ProtobufReferenceResolver) {
    fun resolveMessage(
        identity: QualifiedSchemaName,
        message: Message,
    ): EitherNel<ProtobufResolutionIssue, ResolvedProtobufSchema> = either {
        with(identity) {
            val declaration =
                zipOrAccumulate(
                    { declarationName(message.name) },
                    { parseMessageDeclaration(identity.fullyQualifiedName, message).bindNel() },
                    { checkTypeName(message) },
                    { resolveFields(message) },
                    { resolveOneofs(message) },
                ) { name, parsed, _, fields, oneofs ->
                    ResolvedProtobufMessage(
                        name,
                        parsed.fields.buildEach(fields),
                        parsed.oneofs.buildEach(oneofs),
                        message.reserved,
                    )
                }

            schemaFor(declaration)
        }
    }

    fun resolveEnum(
        identity: QualifiedSchemaName,
        enumDeclaration: Enum,
    ): EitherNel<ProtobufResolutionIssue, ResolvedProtobufSchema> = either {
        with(identity) {
            val declaration =
                zipOrAccumulate(
                    { declarationName(enumDeclaration.name) },
                    { parseEnumDeclaration(identity.fullyQualifiedName, enumDeclaration).bindNel() },
                    { checkTypeName(enumDeclaration) },
                ) { name, parsed, _ ->
                    ResolvedProtobufEnum(
                        name,
                        parsed.values.map { ResolvedProtobufEnumValue(it.name, it.number) },
                        enumDeclaration.reserved,
                    )
                }

            schemaFor(declaration)
        }
    }

    context(identity: QualifiedSchemaName, _: Raise<ProtobufResolutionIssue>)
    private fun declarationName(raw: String): ProtoIdentifier =
        parseIdentifier(identity.fullyQualifiedName, ProtobufResolutionIssue.IdentifierLocation.Declaration, raw)

    context(identity: QualifiedSchemaName, _: Raise<ProtobufResolutionIssue>)
    private fun checkTypeName(declaration: Type) {
        ensure(identity.typeName.value == declaration.name) {
            ProtobufResolutionIssue.SchemaDeclarationNameMismatch(identity.fullyQualifiedName, declaration.name)
        }
    }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveFields(message: Message): List<FieldBuilder> =
        message.fields.mapOrAccumulate { resolveField(it) }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveField(field: MessageField): FieldBuilder =
        when (field) {
            is ScalarField -> scalarField(field)

            is ReferenceField -> referenceField(field)

            is MapField -> mapField(field)
        }

    private fun scalarField(field: ScalarField): FieldBuilder = { parsed ->
        ResolvedProtobufField.Scalar(parsed.name, parsed.number, field.primitive, field.cardinality)
    }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun referenceField(field: ReferenceField): FieldBuilder {
        val reference =
            referenceResolver
                .resolve(field.reference, ProtobufResolutionIssue.ReferenceLocation.Field(field.name))
                .bind()

        return { parsed -> ResolvedProtobufField.Reference(parsed.name, parsed.number, reference, field.cardinality) }
    }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun mapField(field: MapField): FieldBuilder {
        val value = resolveValueType(field.type.value, ProtobufResolutionIssue.ReferenceLocation.MapValue(field.name))

        return { parsed -> ResolvedProtobufField.Map(parsed.name, parsed.number, field.type.key, value) }
    }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveOneofs(message: Message): List<(ParsedOneof) -> ResolvedProtobufOneof> =
        message.oneofs.mapOrAccumulate { resolveOneof(it) }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveOneof(oneof: Oneof): (ParsedOneof) -> ResolvedProtobufOneof {
        val fields = oneof.fields.mapOrAccumulate { resolveOneofField(oneof, it) }

        return { parsed -> ResolvedProtobufOneof(parsed.name, parsed.fields.buildEach(fields)) }
    }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveOneofField(oneof: Oneof, field: OneofField): (ParsedField) -> ResolvedProtobufOneof.Field {
        val type =
            resolveValueType(
                field.fieldType,
                ProtobufResolutionIssue.ReferenceLocation.OneofField(oneof.name, field.name),
            )

        return { parsed -> ResolvedProtobufOneof.Field(parsed.name, parsed.number, type) }
    }

    context(identity: QualifiedSchemaName, _: RaiseAccumulate<ProtobufResolutionIssue>)
    private fun resolveValueType(
        value: ValueType,
        location: ProtobufResolutionIssue.ReferenceLocation,
    ): ResolvedProtobufValueType =
        when (value) {
            is PrimitiveType -> ResolvedProtobufValueType.Primitive(value)

            is Reference -> ResolvedProtobufValueType.Reference(referenceResolver.resolve(value, location).bind())
        }

    context(identity: QualifiedSchemaName)
    private fun schemaFor(declaration: ResolvedProtobufDeclaration): ResolvedProtobufSchema =
        ResolvedProtobufSchema(
            identity,
            declaration,
            dependencies =
                declaration.referencedSchemas().filter { it != identity }.distinct().sortedBy { it.fullyQualifiedName },
        )
}

private typealias FieldBuilder = (ParsedField) -> ResolvedProtobufField

private fun <P, R> List<P>.buildEach(builders: List<(P) -> R>): List<R> =
    zip(builders) { parsed, build -> build(parsed) }

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
