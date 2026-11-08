package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

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
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoDuplicates
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.raiseEach
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.validateMessageReservations

internal fun parseMessageDeclaration(
    schemaName: String,
    message: Message,
): EitherNel<ProtobufResolutionIssue, ParsedMessage> = either {
    val allFields = message.fields + message.oneofs.flatMap(Oneof::fields)

    zipOrAccumulate(
        { parseMessageFields(schemaName, message.fields) },
        { parseOneofs(schemaName, message.oneofs) },
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
        ParsedMessage(fields, oneofs)
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun parseMessageFields(schemaName: String, fields: List<MessageField>): List<ParsedField> =
    fields.mapOrAccumulate {
        parseMessageField(schemaName, it).bindNel()
    }

private fun parseMessageField(
    schemaName: String,
    field: MessageField,
): EitherNel<ProtobufResolutionIssue, ParsedField> = either {
    zipOrAccumulate(
        { parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.Field(field.name), field.name) },
        { parseFieldNumber(schemaName, ProtobufResolutionIssue.FieldLocation.Field(field.name), field.index) },
    ) { name, number ->
        ParsedField(name, number)
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun parseOneofs(schemaName: String, oneofs: List<Oneof>): List<ParsedOneof> = oneofs.mapOrAccumulate {
    parseOneof(schemaName, it).bindNel()
}

private fun parseOneof(schemaName: String, oneof: Oneof): EitherNel<ProtobufResolutionIssue, ParsedOneof> = either {
    zipOrAccumulate(
        { parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.Oneof(oneof.name), oneof.name) },
        { ensure(oneof.fields.isNotEmpty()) { ProtobufResolutionIssue.EmptyOneof(schemaName, oneof.name) } },
        { parseOneofFields(schemaName, oneof) },
    ) { name, _, fields ->
        ParsedOneof(name, fields)
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun parseOneofFields(schemaName: String, oneof: Oneof): List<ParsedField> =
    oneof.fields.mapOrAccumulate { parseOneofField(schemaName, oneof, it).bindNel() }

private fun parseOneofField(
    schemaName: String,
    oneof: Oneof,
    field: OneofField,
): EitherNel<ProtobufResolutionIssue, ParsedField> = either {
    zipOrAccumulate(
        {
            parseIdentifier(
                schemaName,
                ProtobufResolutionIssue.IdentifierLocation.OneofField(oneof.name, field.name),
                field.name,
            )
        },
        {
            parseFieldNumber(
                schemaName,
                ProtobufResolutionIssue.FieldLocation.OneofField(oneof.name, field.name),
                field.index,
            )
        },
    ) { name, number ->
        ParsedField(name, number)
    }
}
