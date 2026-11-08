package io.github.lmliam.microsmith.artifact.schemas.protobuf.render

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufReservations

internal object ProtobufReservedSectionRenderer {
    fun render(reservations: ResolvedProtobufReservations): String? =
        listOfNotNull(renderReservedNames(reservations), renderReservedNumbers(reservations))
            .joinToString("\n")
            .takeIf(String::isNotEmpty)

    private fun renderReservedNames(reservations: ResolvedProtobufReservations): String? =
        reservations.names.takeIf { it.isNotEmpty() }?.joinToString(", ") { "\"$it\"" }?.let { "reserved $it;" }

    private fun renderReservedNumbers(reservations: ResolvedProtobufReservations): String? =
        reservations.spans.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.notation }?.let { "reserved $it;" }
}
