package io.github.lmliam.microsmith.resolve.schemas.protobuf.diagnostics

import io.github.lmliam.microsmith.resolve.diagnostics.ResolutionDiagnostic
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun ProtobufResolutionIssue.ReservationIssue.toDiagnostic(): ResolutionDiagnostic =
    when (this) {
        is ProtobufResolutionIssue.InvalidReservationRange ->
            protobufDiagnostic(
                code = "protobuf.invalid-reservation-range",
                message = "Protobuf schema '$schemaName' has descending reservation range " + "$start..$endInclusive.",
            )

        is ProtobufResolutionIssue.DuplicateReservedNames ->
            protobufDiagnostic(
                code = "protobuf.duplicate-reserved-names",
                message = "Protobuf schema '$schemaName' reserves names more than once: " + names.joinToString(),
            )

        is ProtobufResolutionIssue.ReservedNameCollision ->
            protobufDiagnostic(
                code = "protobuf.reserved-name-collision",
                message = "Protobuf schema '$schemaName' uses reserved names: " + names.joinToString(),
            )

        is ProtobufResolutionIssue.OverlappingReservedNumbers ->
            protobufDiagnostic(
                code = "protobuf.overlapping-reservations",
                message = "Protobuf schema '$schemaName' has overlapping numeric reservations.",
            )

        is ProtobufResolutionIssue.ReservedNumberCollision ->
            protobufDiagnostic(
                code = "protobuf.reserved-number-collision",
                message = "Protobuf schema '$schemaName' uses reserved numbers: " + numbers.joinToString(),
            )
    }
