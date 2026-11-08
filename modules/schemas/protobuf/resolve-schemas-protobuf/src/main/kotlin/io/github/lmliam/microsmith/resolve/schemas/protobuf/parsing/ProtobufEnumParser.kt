package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.bindNel
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.EnumValueNumber
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.ReservedSpan
import io.github.lmliam.microsmith.resolve.schemas.protobuf.validation.checkNoDuplicates

internal fun parseEnumDeclaration(
    schemaName: String,
    enum: Enum,
): EitherNel<ProtobufResolutionIssue, ParsedEnum> = either {
    zipOrAccumulate(
        { checkFirstValueIsZero(schemaName, enum) },
        { parseEnumValues(schemaName, enum) },
        {
            checkNoDuplicates(enum.values.map { it.name }) {
                ProtobufResolutionIssue.DuplicateEnumValueNames(schemaName, it)
            }
        },
        {
            checkNoDuplicates(enum.values.map { it.index }) {
                ProtobufResolutionIssue.DuplicateEnumValueNumbers(schemaName, it)
            }
        },
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

    ensure(first != null) { ProtobufResolutionIssue.EmptyEnum(schemaName) }
    ensure(first.index == 0) { ProtobufResolutionIssue.EnumFirstValueMustBeZero(schemaName, first.name, first.index) }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun parseEnumValues(schemaName: String, enum: Enum): List<ParsedEnumValue> =
    enum.values.mapOrAccumulate {
        ParsedEnumValue(
            parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.EnumValue(it.name), it.name),
            EnumValueNumber(it.index),
        )
    }
