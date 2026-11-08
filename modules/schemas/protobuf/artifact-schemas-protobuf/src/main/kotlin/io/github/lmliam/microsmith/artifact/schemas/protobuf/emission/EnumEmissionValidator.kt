package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufEnum

internal fun validateEnum(enum: ResolvedProtobufEnum) {
    require(enum.values.isNotEmpty()) { "Enum '${enum.name}' must contain at least one value" }
    require(enum.values.first().number.value == 0) { "Enum '${enum.name}' must declare first value at index 0" }

    requireUniqueEnumValueNames(enum)
    requireUniqueEnumValueIndexes(enum)
    validateReservedUsage(enum)
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
