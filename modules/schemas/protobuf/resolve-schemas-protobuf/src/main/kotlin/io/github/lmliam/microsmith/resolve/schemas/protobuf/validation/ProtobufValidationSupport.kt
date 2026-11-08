package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import arrow.core.raise.Raise
import arrow.core.raise.RaiseAccumulate
import arrow.core.raise.context.ensure
import arrow.core.raise.context.mapOrAccumulate
import arrow.core.raise.context.raise
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtoIdentifier
import io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers.FieldNumber

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
    if (FieldNumber.parse(number).isLeft()) {
        add(
            ProtobufResolutionIssue.InvalidFieldNumber(
                schemaName,
                location,
                number,
            )
        )
    }
}

internal fun <T : Comparable<T>> duplicateValues(values: List<T>): List<T> =
    values.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.sorted()
