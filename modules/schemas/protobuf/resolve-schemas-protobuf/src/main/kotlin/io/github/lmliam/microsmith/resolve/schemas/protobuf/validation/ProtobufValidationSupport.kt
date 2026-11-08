package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.bind
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.raise
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier

internal const val MIN_PROTOBUF_FIELD_NUMBER = 1
internal const val MAX_PROTOBUF_FIELD_NUMBER = 536_870_911

internal val FORBIDDEN_PROTOBUF_FIELD_NUMBER_RANGE = 19_000..19_999

context(_: Raise<ProtobufResolutionIssue>)
internal fun parseIdentifier(
    schemaName: String,
    location: ProtobufResolutionIssue.IdentifierLocation,
    raw: String,
): ProtoIdentifier =
    ProtoIdentifier.parse(raw).mapLeft { ProtobufResolutionIssue.InvalidIdentifier(schemaName, location, it) }.bind()

context(_: Raise<ProtobufResolutionIssue>)
internal fun checkFieldNumber(schemaName: String, location: ProtobufResolutionIssue.FieldLocation, number: Int) {
    ensure(isProtobufFieldNumber(number)) { ProtobufResolutionIssue.InvalidFieldNumber(schemaName, location, number) }
}

context(_: Raise<ProtobufResolutionIssue>)
internal fun <T : Comparable<T>> checkNoDuplicates(values: List<T>, issue: (List<T>) -> ProtobufResolutionIssue) {
    val duplicates = duplicateValues(values)

    ensure(duplicates.isEmpty()) { issue(duplicates) }
}

context(_: RaiseAccumulate<ProtobufResolutionIssue>)
internal fun List<ProtobufResolutionIssue>.raiseEach() {
    mapOrAccumulate { raise(it) }
}

internal fun MutableList<ProtobufResolutionIssue>.validateIdentifier(
    schemaName: String,
    location: ProtobufResolutionIssue.IdentifierLocation,
    value: String,
) {
    if (ProtoIdentifier.parse(value).isLeft()) {
        add(
            ProtobufResolutionIssue.InvalidIdentifier(
                schemaName,
                location,
                value,
            )
        )
    }
}

internal fun MutableList<ProtobufResolutionIssue>.validateFieldNumber(
    schemaName: String,
    location: ProtobufResolutionIssue.FieldLocation,
    number: Int,
) {
    if (!isProtobufFieldNumber(number)) {
        add(
            ProtobufResolutionIssue.InvalidFieldNumber(
                schemaName,
                location,
                number,
            )
        )
    }
}

private fun isProtobufFieldNumber(number: Int): Boolean =
    number in MIN_PROTOBUF_FIELD_NUMBER..MAX_PROTOBUF_FIELD_NUMBER && number !in FORBIDDEN_PROTOBUF_FIELD_NUMBER_RANGE

internal fun <T : Comparable<T>> duplicateValues(values: List<T>): List<T> =
    values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
