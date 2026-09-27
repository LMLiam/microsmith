package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun validateReservationNames(
    schemaName: String,
    reservations: List<Reserved>,
    usedNames: List<String>,
): List<ProtobufResolutionIssue> = buildList {
    val names =
        reservations
            .filterIsInstance<ReservedName>()
            .map(ReservedName::name)

    names.forEach { name ->
        validateIdentifier(
            schemaName,
            ProtobufResolutionIssue.IdentifierLocation.Reservation(
                name,
            ),
            name,
        )
    }

    duplicateValues(names)
        .takeIf(List<String>::isNotEmpty)
        ?.let { duplicates ->
            add(
                ProtobufResolutionIssue.DuplicateReservedNames(
                    schemaName,
                    duplicates,
                ),
            )
        }

    names
        .toSet()
        .intersect(usedNames.toSet())
        .sorted()
        .takeIf(List<String>::isNotEmpty)
        ?.let { collisions ->
            add(
                ProtobufResolutionIssue.ReservedNameCollision(
                    schemaName,
                    collisions,
                ),
            )
        }
}
