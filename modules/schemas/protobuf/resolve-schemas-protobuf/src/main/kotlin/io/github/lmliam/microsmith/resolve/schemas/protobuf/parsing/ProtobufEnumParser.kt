package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.bindNel
import arrow.core.raise.context.either
import arrow.core.raise.context.ensureNotNull
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.zipOrAccumulate
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkEnumStartsAtZero
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueEnumValueNames
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkUniqueEnumValueNumbers

internal fun parseEnumDeclaration(
    schemaName: String,
    enum: Enum,
): EitherNel<ProtobufResolutionIssue, ParsedEnum> = either {
    zipOrAccumulate(
        { checkFirstValueIsZero(schemaName, enum) },
        { parseEnumValues(schemaName, enum) },
        { checking(schemaName) { checkUniqueEnumValueNames(enum.values.map { it.name }) } },
        { checking(schemaName) { checkUniqueEnumValueNumbers(enum.values.map { it.index }) } },
        {
            parseReservations(
                    schemaName,
                    enum.reserved,
                    ReservedSpan.Domain.ENUM,
                    usedNames = enum.values.map { it.name },
                    usedNumbers = enum.values.map { it.index },
                )
                .bindNel()
        },
    ) { _, values, _, _, reservations ->
        ParsedEnum(values, reservations)
    }
}

context(_: Raise<ProtobufResolutionIssue>)
private fun checkFirstValueIsZero(schemaName: String, enum: Enum) {
    val first = enum.values.firstOrNull()

    if (first != null) {
        checking(schemaName) { checkEnumStartsAtZero(first.name, first.index) }
    }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun parseEnumValues(schemaName: String, enum: Enum): NonEmptyList<ParsedEnumValue> {
    val values = ensureNotNull(enum.values.toNonEmptyListOrNull()) { ProtobufResolutionIssue.EmptyEnum(schemaName) }

    return values.mapOrAccumulate {
        ParsedEnumValue(
            parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.EnumValue(it.name), it.name),
            EnumValueNumber(it.index),
        )
    }
}
