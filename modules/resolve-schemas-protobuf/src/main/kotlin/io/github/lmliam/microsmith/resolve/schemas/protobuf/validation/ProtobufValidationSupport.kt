package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation

internal const val MIN_PROTOBUF_FIELD_NUMBER = 1
internal const val MAX_PROTOBUF_FIELD_NUMBER = 536_870_911

internal val FORBIDDEN_PROTOBUF_FIELD_NUMBER_RANGE =
    19_000..19_999

internal fun MutableList<ProtobufResolutionIssue>.validateIdentifier(
    schemaName: String,
    location: ProtobufResolutionIssue.IdentifierLocation,
    value: String,
) {
    if (!ProtobufNameValidation.isIdentifier(value)) {
        add(
            ProtobufResolutionIssue.InvalidIdentifier(
                schemaName,
                location,
                value,
            ),
        )
    }
}

internal fun MutableList<ProtobufResolutionIssue>.validateFieldNumber(
    schemaName: String,
    location: ProtobufResolutionIssue.FieldLocation,
    number: Int,
) {
    if (
        number !in MIN_PROTOBUF_FIELD_NUMBER..MAX_PROTOBUF_FIELD_NUMBER ||
        number in FORBIDDEN_PROTOBUF_FIELD_NUMBER_RANGE
    ) {
        add(
            ProtobufResolutionIssue.InvalidFieldNumber(
                schemaName,
                location,
                number,
            ),
        )
    }
}

internal fun <T : Comparable<T>> duplicateValues(values: List<T>): List<T> = values
    .groupingBy { it }.eachCount()
    .filterValues { it > 1 }
    .keys
    .sorted()
