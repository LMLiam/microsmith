package io.github.lmliam.microsmith.artifact.schemas.protobuf.render
import io.github.lmliam.microsmith.artifact.schemas.protobuf.emission.ProtobufEmissionInvariantException
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax

internal object ProtobufReservedSectionRenderer {
    fun render(entries: List<Reserved>): String? {
        if (entries.isEmpty()) {
            return null
        }

        return listOfNotNull(renderReservedNames(entries), renderNumericReservedEntries(entries)).joinToString("\n")
    }

    private fun renderReservedNames(entries: List<Reserved>): String? = entries
        .filterIsInstance<ReservedName>()
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { "\"${it.name}\"" }
        ?.let { "reserved $it;" }

    private fun renderNumericReservedEntries(entries: List<Reserved>): String? = entries
        .filterNot { it is ReservedName }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(", ", transform = ::renderNumericReserved)
        ?.let { "reserved $it;" }

    private fun renderNumericReserved(reserved: Reserved): String = when (reserved) {
        is ReservedIndex -> reserved.index.toString()

        is ReservedRange -> "${reserved.indexRange.first} to ${reserved.indexRange.last}"

        is ReservedToMax -> "${reserved.from} to max"

        is ReservedName -> throw ProtobufEmissionInvariantException(
            "Reserved names are rendered by renderReservedNames().",
        )
    }
}
