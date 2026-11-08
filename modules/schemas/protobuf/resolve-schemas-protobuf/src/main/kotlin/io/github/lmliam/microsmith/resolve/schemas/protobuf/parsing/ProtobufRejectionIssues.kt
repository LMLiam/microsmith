package io.github.lmliam.microsmith.resolve.schemas.protobuf.parsing

import arrow.core.raise.Raise
import arrow.core.raise.context.withError
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection

context(_: Raise<ProtobufResolutionIssue>)
internal fun <A> checking(schemaName: String, rule: context(Raise<ResolvedProtobufRejection>) () -> A): A =
    withError({ rejection -> rejection.toIssue(schemaName) }, rule)

internal fun ResolvedProtobufRejection.toIssue(schemaName: String): ProtobufResolutionIssue =
    when (this) {
        is ResolvedProtobufRejection.DuplicateFieldNames ->
            ProtobufResolutionIssue.DuplicateFieldNames(schemaName, names)

        is ResolvedProtobufRejection.DuplicateFieldNumbers ->
            ProtobufResolutionIssue.DuplicateFieldNumbers(schemaName, numbers)

        is ResolvedProtobufRejection.DuplicateOneofNames ->
            ProtobufResolutionIssue.DuplicateOneofNames(schemaName, names)

        is ResolvedProtobufRejection.FieldJsonNameCollision ->
            ProtobufResolutionIssue.FieldJsonNameCollision(schemaName, jsonName, fieldNames)

        is ResolvedProtobufRejection.MessageSymbolCollision ->
            ProtobufResolutionIssue.SymbolNameCollision(
                scope = schemaName,
                name = name,
                declarations = declarations.map { it.declaredIn(schemaName) },
            )

        is ResolvedProtobufRejection.EnumFirstValueMustBeZero ->
            ProtobufResolutionIssue.EnumFirstValueMustBeZero(schemaName, firstValueName, number)

        is ResolvedProtobufRejection.DuplicateEnumValueNames ->
            ProtobufResolutionIssue.DuplicateEnumValueNames(schemaName, names)

        is ResolvedProtobufRejection.DuplicateEnumValueNumbers ->
            ProtobufResolutionIssue.DuplicateEnumValueNumbers(schemaName, numbers)

        is ResolvedProtobufRejection.EnumValueNameCollision ->
            ProtobufResolutionIssue.EnumValueNameCollision(schemaName, valueNames)

        is ResolvedProtobufRejection.DuplicateReservedNames ->
            ProtobufResolutionIssue.DuplicateReservedNames(schemaName, names)

        ResolvedProtobufRejection.OverlappingReservedNumbers ->
            ProtobufResolutionIssue.OverlappingReservedNumbers(schemaName)

        is ResolvedProtobufRejection.ReservedNameCollision ->
            ProtobufResolutionIssue.ReservedNameCollision(schemaName, names)

        is ResolvedProtobufRejection.ReservedNumberCollision ->
            ProtobufResolutionIssue.ReservedNumberCollision(schemaName, numbers)

        is ResolvedProtobufRejection.ReservedNumberOutOfRange ->
            ProtobufResolutionIssue.InvalidFieldNumber(
                schemaName,
                ProtobufResolutionIssue.FieldLocation.Reservation(notation),
                number,
            )
    }

internal fun ResolvedProtobufRejection.MessageSymbol.declaredIn(
    schemaName: String
): ProtobufResolutionIssue.SymbolDeclaration =
    when (this) {
        is ResolvedProtobufRejection.MessageSymbol.Field ->
            ProtobufResolutionIssue.SymbolDeclaration.Field(schemaName, fieldName)

        is ResolvedProtobufRejection.MessageSymbol.Oneof ->
            ProtobufResolutionIssue.SymbolDeclaration.Oneof(schemaName, oneofName)

        is ResolvedProtobufRejection.MessageSymbol.MapEntry ->
            ProtobufResolutionIssue.SymbolDeclaration.MapEntry(schemaName, fieldName)
    }
