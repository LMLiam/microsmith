package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun validateMessageReservationSpan(
    schemaName: String,
    reservation: Reserved,
    issues: MutableList<ProtobufResolutionIssue>,
): IntRange? = when (reservation) {
    is ReservedName ->
        null

    is ReservedIndex -> {
        issues.validateFieldNumber(
            schemaName,
            ProtobufResolutionIssue.FieldLocation.Reservation(
                reservation.index.toString(),
            ),
            reservation.index,
        )

        reservation.index..reservation.index
    }

    is ReservedRange ->
        validateMessageReservationRange(
            schemaName,
            reservation.indexRange,
            issues,
        )

    is ReservedToMax -> {
        issues.validateFieldNumber(
            schemaName,
            ProtobufResolutionIssue.FieldLocation.Reservation(
                "${reservation.from} to max",
            ),
            reservation.from,
        )

        reservation.from..MAX_PROTOBUF_FIELD_NUMBER
    }
}

internal fun validateEnumReservationSpan(
    schemaName: String,
    reservation: Reserved,
    issues: MutableList<ProtobufResolutionIssue>,
): IntRange? = when (reservation) {
    is ReservedName ->
        null

    is ReservedIndex ->
        reservation.index..reservation.index

    is ReservedRange ->
        validateEnumReservationRange(
            schemaName,
            reservation.indexRange,
            issues,
        )

    is ReservedToMax ->
        reservation.from..Int.MAX_VALUE
}

private fun validateMessageReservationRange(
    schemaName: String,
    range: IntRange,
    issues: MutableList<ProtobufResolutionIssue>,
): IntRange? {
    if (!validateAscendingReservationRange(schemaName, range, issues)) {
        return null
    }

    val location =
        ProtobufResolutionIssue.FieldLocation.Reservation(
            range.toString(),
        )

    issues.validateFieldNumber(
        schemaName,
        location,
        range.first,
    )

    issues.validateFieldNumber(
        schemaName,
        location,
        range.last,
    )

    return range
}

private fun validateEnumReservationRange(
    schemaName: String,
    range: IntRange,
    issues: MutableList<ProtobufResolutionIssue>,
): IntRange? = range.takeIf {
    validateAscendingReservationRange(
        schemaName,
        range,
        issues,
    )
}

private fun validateAscendingReservationRange(
    schemaName: String,
    range: IntRange,
    issues: MutableList<ProtobufResolutionIssue>,
): Boolean {
    if (range.first <= range.last) {
        return true
    }

    issues +=
        ProtobufResolutionIssue.InvalidReservationRange(
            schemaName,
            range.first,
            range.last,
        )

    return false
}
