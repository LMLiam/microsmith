package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun validateMessageReservations(
    schemaName: String,
    reservations: List<Reserved>,
    usedNames: List<String>,
    usedNumbers: List<Int>,
): List<ProtobufResolutionIssue> = buildList {
    addAll(
        validateReservationNames(
            schemaName,
            reservations,
            usedNames,
        ),
    )

    val spans =
        reservations.mapNotNull { reservation ->
            validateMessageReservationSpan(
                schemaName,
                reservation,
                this,
            )
        }

    addAll(
        validateReservationNumbers(
            schemaName,
            spans,
            usedNumbers,
        ),
    )
}

internal fun validateEnumReservations(
    schemaName: String,
    reservations: List<Reserved>,
    usedNames: List<String>,
    usedNumbers: List<Int>,
): List<ProtobufResolutionIssue> = buildList {
    addAll(
        validateReservationNames(
            schemaName,
            reservations,
            usedNames,
        ),
    )

    val spans =
        reservations.mapNotNull { reservation ->
            validateEnumReservationSpan(
                schemaName,
                reservation,
                this,
            )
        }

    addAll(
        validateReservationNumbers(
            schemaName,
            spans,
            usedNumbers,
        ),
    )
}
