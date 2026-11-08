package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.EitherNel
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.bindNel
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MessageField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

internal fun validateMessageDeclaration(
    schemaName: String,
    message: Message,
): EitherNel<ProtobufResolutionIssue, MessageNames> = either {
    val allFields = message.fields + message.oneofs.flatMap(Oneof::fields)

    zipOrAccumulate(
        { validateMessageFields(schemaName, message.fields) },
        { validateOneofs(schemaName, message.oneofs) },
        {
            checkNoDuplicates(allFields.map(Field::name)) {
                ProtobufResolutionIssue.DuplicateFieldNames(schemaName, it)
            }
        },
        {
            checkNoDuplicates(allFields.map(Field::index)) {
                ProtobufResolutionIssue.DuplicateFieldNumbers(schemaName, it)
            }
        },
        {
            checkNoDuplicates(message.oneofs.map(Oneof::name)) {
                ProtobufResolutionIssue.DuplicateOneofNames(schemaName, it)
            }
        },
        {
            validateMessageReservations(
                    schemaName,
                    message.reserved,
                    usedNames = allFields.map(Field::name),
                    usedNumbers = allFields.map(Field::index),
                )
                .raiseEach()
        },
    ) { fields, oneofs, _, _, _, _ ->
        MessageNames(fields, oneofs)
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun validateMessageFields(schemaName: String, fields: List<MessageField>): List<ProtoIdentifier> =
    fields.mapOrAccumulate {
        validateMessageField(schemaName, it).bindNel()
    }

private fun validateMessageField(
    schemaName: String,
    field: MessageField,
): EitherNel<ProtobufResolutionIssue, ProtoIdentifier> = either {
    zipOrAccumulate(
        { parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.Field(field.name), field.name) },
        { checkFieldNumber(schemaName, ProtobufResolutionIssue.FieldLocation.Field(field.name), field.index) },
    ) { name, _ ->
        name
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun validateOneofs(schemaName: String, oneofs: List<Oneof>): List<OneofNames> = oneofs.mapOrAccumulate {
    validateOneof(schemaName, it).bindNel()
}

private fun validateOneof(schemaName: String, oneof: Oneof): EitherNel<ProtobufResolutionIssue, OneofNames> = either {
    zipOrAccumulate(
        { parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.Oneof(oneof.name), oneof.name) },
        { ensure(oneof.fields.isNotEmpty()) { ProtobufResolutionIssue.EmptyOneof(schemaName, oneof.name) } },
        { validateOneofFields(schemaName, oneof) },
    ) { name, _, fields ->
        OneofNames(name, fields)
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun validateOneofFields(schemaName: String, oneof: Oneof): List<ProtoIdentifier> =
    oneof.fields.mapOrAccumulate { validateOneofField(schemaName, oneof, it).bindNel() }

private fun validateOneofField(
    schemaName: String,
    oneof: Oneof,
    field: OneofField,
): EitherNel<ProtobufResolutionIssue, ProtoIdentifier> = either {
    zipOrAccumulate(
        {
            parseIdentifier(
                schemaName,
                ProtobufResolutionIssue.IdentifierLocation.OneofField(oneof.name, field.name),
                field.name,
            )
        },
        {
            checkFieldNumber(
                schemaName,
                ProtobufResolutionIssue.FieldLocation.OneofField(oneof.name, field.name),
                field.index,
            )
        },
    ) { name, _ ->
        name
    }
}
