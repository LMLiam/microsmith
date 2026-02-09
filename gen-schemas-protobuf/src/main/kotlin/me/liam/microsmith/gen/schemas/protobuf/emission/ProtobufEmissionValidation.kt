package me.liam.microsmith.gen.schemas.protobuf.emission

import me.liam.microsmith.dsl.schemas.protobuf.ProtobufSchema
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.oneof.Oneof
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Reserved
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedIndex
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedName
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedRange
import me.liam.microsmith.dsl.schemas.protobuf.reserved.ReservedToMax
import me.liam.microsmith.dsl.schemas.protobuf.types.Enum
import me.liam.microsmith.dsl.schemas.protobuf.types.EnumValue
import me.liam.microsmith.dsl.schemas.protobuf.types.Message
import me.liam.microsmith.gen.schemas.protobuf.names.ProtobufNameValidation

/**
 * Validation helpers for protobuf emission.
 *
 * These checks protect emitter assumptions when models are instantiated programmatically
 * (outside the higher-level DSL builders).
 */
internal fun ProtobufSchema.validateForEmission() {
    when (val currentType = schema) {
        is Message -> currentType.validateForEmission()
        is Enum -> currentType.validateForEmission()
    }
}

private fun Message.validateForEmission() {
    ProtobufNameValidation.requireIdentifier(name, "Message name")

    fields.forEach { it.validateForEmission() }
    oneofs.forEach { it.validateForEmission() }
    reserved.forEach { it.validateForEmission() }

    requireUniqueFieldNames()
    requireUniqueFieldNumbers()
}

private fun Message.requireUniqueFieldNames() {
    val usages =
        buildList {
            fields.forEach { add(it.name to "field '${it.name}'") }
            oneofs.forEach { oneof ->
                oneof.fields.forEach { field ->
                    add(field.name to "oneof '${oneof.name}' field '${field.name}'")
                }
            }
        }
    val duplicates = usages.groupBy(keySelector = { it.first }, valueTransform = { it.second }).filterValues { it.size > 1 }

    require(duplicates.isEmpty()) {
        val details =
            duplicates
                .toSortedMap()
                .entries
                .joinToString("; ") { (duplicateName, locations) ->
                    "$duplicateName (${locations.joinToString()})"
                }
        "Duplicate field names in message '$name': $details"
    }
}

private fun Message.requireUniqueFieldNumbers() {
    val usages =
        buildList {
            fields.forEach { add(it.index to "field '${it.name}'") }
            oneofs.forEach { oneof ->
                oneof.fields.forEach { field ->
                    add(field.index to "oneof '${oneof.name}' field '${field.name}'")
                }
            }
        }
    val duplicates = usages.groupBy(keySelector = { it.first }, valueTransform = { it.second }).filterValues { it.size > 1 }

    require(duplicates.isEmpty()) {
        val details =
            duplicates
                .toSortedMap()
                .entries
                .joinToString("; ") { (duplicateNumber, locations) ->
                    "$duplicateNumber (${locations.joinToString()})"
                }
        "Duplicate field numbers in message '$name': $details"
    }
}

private fun Enum.validateForEmission() {
    ProtobufNameValidation.requireIdentifier(name, "Enum name")
    require(values.isNotEmpty()) { "Enum '$name' must contain at least one value." }
    require(values.first().index == 0) { "Enum '$name' must declare first value at index 0." }

    values.forEach { it.validateForEmission() }
    reserved.forEach { it.validateForEmission() }

    requireUniqueEnumValueNames()
    requireUniqueEnumValueIndexes()
}

private fun Enum.requireUniqueEnumValueNames() {
    val duplicates = values.groupBy { it.name }.filterValues { it.size > 1 }
    require(duplicates.isEmpty()) {
        val names = duplicates.keys.sorted().joinToString(", ")
        "Enum '$name' has duplicate value names: $names"
    }
}

private fun Enum.requireUniqueEnumValueIndexes() {
    val duplicates = values.groupBy { it.index }.filterValues { it.size > 1 }
    require(duplicates.isEmpty()) {
        val indexes = duplicates.keys.sorted().joinToString(", ")
        "Enum '$name' has duplicate value indexes: $indexes"
    }
}

private fun EnumValue.validateForEmission() {
    ProtobufNameValidation.requireIdentifier(name, "Enum value name")
}

private fun Oneof.validateForEmission() {
    ProtobufNameValidation.requireIdentifier(name, "Oneof name")
    require(fields.isNotEmpty()) { "Oneof '$name' must contain at least one field." }
    fields.forEach { it.validateForEmission() }
}

private fun Field.validateForEmission() {
    ProtobufNameValidation.requireIdentifier(name, "Field name")
    ProtobufFieldNumbers.requireValidFieldNumber(index, "Field '$name' index")
    when (this) {
        is ScalarField -> Unit
        is ReferenceField -> reference.validateForEmission()
        is MapField -> (type.value as? Reference)?.validateForEmission()
        is OneofField -> invalidTopLevelOneofField(name)
    }
}

private fun OneofField.validateForEmission() {
    ProtobufNameValidation.requireIdentifier(name, "Oneof field name")
    ProtobufFieldNumbers.requireValidFieldNumber(index, "Oneof field '$name' index")
    (fieldType as? Reference)?.validateForEmission()
}

private fun Reference.validateForEmission() {
    ProtobufNameValidation.normalizeQualifiedName(name, "Reference name")
}

private fun Reserved.validateForEmission() {
    when (this) {
        is ReservedIndex -> ProtobufFieldNumbers.requireValidFieldNumber(index, "Reserved index")
        is ReservedRange -> {
            require(indexRange.first <= indexRange.last) {
                "Reserved range must be ascending, but was $indexRange."
            }
            ProtobufFieldNumbers.requireValidFieldNumber(indexRange.first, "Reserved range start")
            ProtobufFieldNumbers.requireValidFieldNumber(indexRange.last, "Reserved range end")
        }
        is ReservedToMax -> ProtobufFieldNumbers.requireValidFieldNumber(from, "Reserved-to-max start")
        is ReservedName -> ProtobufNameValidation.requireIdentifier(name, "Reserved name")
    }
}
