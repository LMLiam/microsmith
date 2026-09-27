package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun validateEnumDeclaration(schemaName: String, enum: Enum): List<ProtobufResolutionIssue> = buildList {
    if (enum.values.isEmpty()) {
        add(ProtobufResolutionIssue.EmptyEnum(schemaName))
    } else if (enum.values.first().index != 0) {
        val first = enum.values.first()

        add(
            ProtobufResolutionIssue.EnumFirstValueMustBeZero(
                schemaName,
                first.name,
                first.index,
            ),
        )
    }

    enum.values.forEach { value ->
        validateIdentifier(
            schemaName,
            ProtobufResolutionIssue.IdentifierLocation.EnumValue(value.name),
            value.name,
        )
    }

    duplicateValues(enum.values.map { it.name }).takeIf(List<String>::isNotEmpty)?.let {
        add(
            ProtobufResolutionIssue.DuplicateEnumValueNames(
                schemaName,
                it,
            ),
        )
    }

    duplicateValues(enum.values.map { it.index }).takeIf(List<Int>::isNotEmpty)?.let {
        add(
            ProtobufResolutionIssue.DuplicateEnumValueNumbers(
                schemaName,
                it,
            ),
        )
    }

    addAll(
        validateEnumReservations(
            schemaName,
            enum.reserved,
            usedNames = enum.values.map { it.name },
            usedNumbers = enum.values.map { it.index },
        ),
    )
}
