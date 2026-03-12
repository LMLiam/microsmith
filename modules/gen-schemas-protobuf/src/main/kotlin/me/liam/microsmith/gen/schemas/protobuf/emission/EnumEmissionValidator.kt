package me.liam.microsmith.gen.schemas.protobuf.emission

import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.EnumValue
import me.liam.microsmith.gen.schemas.protobuf.names.ProtobufNameValidation

internal object EnumEmissionValidator {
    fun validate(enum: Enum) {
        ProtobufNameValidation.requireIdentifier(enum.name, "Enum name")
        require(enum.values.isNotEmpty()) { "Enum '${enum.name}' must contain at least one value." }
        require(enum.values.first().index == 0) { "Enum '${enum.name}' must declare first value at index 0." }

        enum.values.forEach(::validate)
        enum.reserved.forEach(ReservedEmissionValidator::validate)

        requireUniqueNames(enum)
        requireUniqueIndexes(enum)
        ReservedUsageCollisionValidator.validate(enum)
    }

    private fun validate(value: EnumValue) {
        ProtobufNameValidation.requireIdentifier(value.name, "Enum value name")
    }

    private fun requireUniqueNames(enum: Enum) {
        val duplicates = enum.values.groupBy(EnumValue::name).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val names = duplicates.keys.sorted().joinToString(", ")
            "Enum '${enum.name}' has duplicate value names: $names"
        }
    }

    private fun requireUniqueIndexes(enum: Enum) {
        val duplicates = enum.values.groupBy(EnumValue::index).filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val indexes = duplicates.keys.sorted().joinToString(", ")
            "Enum '${enum.name}' has duplicate value indexes: $indexes"
        }
    }
}
