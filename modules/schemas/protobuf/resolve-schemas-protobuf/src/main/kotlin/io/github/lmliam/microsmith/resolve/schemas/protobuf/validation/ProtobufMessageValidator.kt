package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun validateMessageDeclaration(schemaName: String, message: Message): List<ProtobufResolutionIssue> =
    buildList {
        message.fields.forEach { field ->
            validateMessageField(
                schemaName,
                field,
            )
        }

        message.oneofs.forEach { oneof ->
            validateOneof(
                schemaName,
                oneof,
            )
        }

        val allFields = message.fields + message.oneofs.flatMap(Oneof::fields)

        duplicateValues(allFields.map(Field::name)).takeIf(List<String>::isNotEmpty)?.let {
            add(
                ProtobufResolutionIssue.DuplicateFieldNames(
                    schemaName,
                    it,
                )
            )
        }

        duplicateValues(allFields.map(Field::index)).takeIf(List<Int>::isNotEmpty)?.let {
            add(
                ProtobufResolutionIssue.DuplicateFieldNumbers(
                    schemaName,
                    it,
                )
            )
        }

        duplicateValues(message.oneofs.map(Oneof::name)).takeIf(List<String>::isNotEmpty)?.let {
            add(
                ProtobufResolutionIssue.DuplicateOneofNames(
                    schemaName,
                    it,
                )
            )
        }

        addAll(
            validateMessageReservations(
                schemaName,
                message.reserved,
                usedNames = allFields.map(Field::name),
                usedNumbers = allFields.map(Field::index),
            )
        )
    }

private fun MutableList<ProtobufResolutionIssue>.validateMessageField(schemaName: String, field: Field) {
    validateIdentifier(
        schemaName,
        ProtobufResolutionIssue.IdentifierLocation.Field(field.name),
        field.name,
    )

    validateFieldNumber(
        schemaName,
        ProtobufResolutionIssue.FieldLocation.Field(field.name),
        field.index,
    )
}

private fun MutableList<ProtobufResolutionIssue>.validateOneof(schemaName: String, oneof: Oneof) {
    validateIdentifier(
        schemaName,
        ProtobufResolutionIssue.IdentifierLocation.Oneof(oneof.name),
        oneof.name,
    )

    if (oneof.fields.isEmpty()) {
        add(
            ProtobufResolutionIssue.EmptyOneof(
                schemaName,
                oneof.name,
            )
        )
    }

    oneof.fields.forEach { field ->
        validateIdentifier(
            schemaName,
            ProtobufResolutionIssue.IdentifierLocation.OneofField(
                oneof.name,
                field.name,
            ),
            field.name,
        )

        validateFieldNumber(
            schemaName,
            ProtobufResolutionIssue.FieldLocation.OneofField(
                oneof.name,
                field.name,
            ),
            field.index,
        )
    }
}
