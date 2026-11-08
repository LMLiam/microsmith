package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.either
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.zipOrAccumulate
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Enum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

internal fun validateEnumDeclaration(
    schemaName: String,
    enum: Enum,
): EitherNel<ProtobufResolutionIssue, List<ProtoIdentifier>> = either {
    zipOrAccumulate(
        { checkFirstValueIsZero(schemaName, enum) },
        { validateEnumValueNames(schemaName, enum) },
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
            validateEnumReservations(
                    schemaName,
                    enum.reserved,
                    usedNames = enum.values.map { it.name },
                    usedNumbers = enum.values.map { it.index },
                )
                .raiseEach()
        },
    ) { _, names, _, _, _ ->
        names
    }
}

context(_: Raise<ProtobufResolutionIssue>)
private fun checkFirstValueIsZero(schemaName: String, enum: Enum) {
    val first = enum.values.firstOrNull()

    ensure(first != null) { ProtobufResolutionIssue.EmptyEnum(schemaName) }
    ensure(first.index == 0) { ProtobufResolutionIssue.EnumFirstValueMustBeZero(schemaName, first.name, first.index) }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
private fun validateEnumValueNames(schemaName: String, enum: Enum): List<ProtoIdentifier> =
    enum.values.mapOrAccumulate {
        parseIdentifier(schemaName, ProtobufResolutionIssue.IdentifierLocation.EnumValue(it.name), it.name)
    }
