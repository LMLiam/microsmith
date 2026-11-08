package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation

internal fun validateEnum(enum: ResolvedProtobufEnum) {
    require(enum.values.isNotEmpty()) { "Enum '${enum.name}' must contain at least one value" }
    require(enum.values.first().number.value == 0) { "Enum '${enum.name}' must declare first value at index 0" }

    enum.reservations.forEach(::validateEnumReserved)

    requireUniqueEnumValueNames(enum)
    requireUniqueEnumValueIndexes(enum)
    validateReservedUsage(enum)
}

private fun validateEnumReserved(reserved: Reserved) {
    when (reserved) {
        is ReservedName -> ProtobufNameValidation.requireIdentifier(reserved.name, "Reserved name")

        is ReservedRange ->
            require(reserved.indexRange.first <= reserved.indexRange.last) {
                "Reserved range must be ascending, but was ${reserved.indexRange}"
            }

        is ReservedIndex,
        is ReservedToMax -> Unit
    }
}

private fun requireUniqueEnumValueNames(enum: ResolvedProtobufEnum) {
    val duplicates = enum.values.groupBy { it.name.value }.filterValues { it.size > 1 }
    require(duplicates.isEmpty()) {
        val names = duplicates.keys.sorted().joinToString(", ")
        "Enum '${enum.name}' has duplicate value names: $names"
    }
}

private fun requireUniqueEnumValueIndexes(enum: ResolvedProtobufEnum) {
    val duplicates = enum.values.groupBy { it.number.value }.filterValues { it.size > 1 }
    require(duplicates.isEmpty()) {
        val indexes = duplicates.keys.sorted().joinToString(", ")
        "Enum '${enum.name}' has duplicate value indexes: $indexes"
    }
}
