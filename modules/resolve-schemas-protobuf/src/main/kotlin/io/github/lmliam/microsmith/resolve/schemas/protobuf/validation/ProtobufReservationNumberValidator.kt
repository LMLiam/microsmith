package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ProtobufResolutionIssue

internal fun validateReservationNumbers(
    schemaName: String,
    spans: List<IntRange>,
    usedNumbers: List<Int>,
): List<ProtobufResolutionIssue> = buildList {
    val overlapping =
        spans
            .sortedBy(IntRange::first)
            .zipWithNext()
            .any { (left, right) ->
                left.last >= right.first
            }

    if (overlapping) {
        add(
            ProtobufResolutionIssue.OverlappingReservedNumbers(
                schemaName,
            ),
        )
    }

    usedNumbers
        .distinct()
        .filter { number ->
            spans.any { span ->
                number in span
            }
        }.sorted()
        .takeIf(List<Int>::isNotEmpty)
        ?.let { collisions ->
            add(
                ProtobufResolutionIssue.ReservedNumberCollision(
                    schemaName,
                    collisions,
                ),
            )
        }
}
