package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufMessage
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation

internal fun validateMessage(message: ResolvedProtobufMessage) {
    ProtobufNameValidation.requireIdentifier(message.name, "Message name")
    message.fields.forEach(::validateField)
    message.oneofs.forEach(::validateOneof)
    message.reservations.forEach(::validateReserved)

    requireUniqueFieldNames(message)
    requireUniqueFieldNumbers(message)
    requireUniqueOneofNames(message)
    validateReservedUsage(message)
}

private fun requireUniqueFieldNames(message: ResolvedProtobufMessage) {
    requireUniqueUsages(message.name, "field names", collectFieldNameUsages(message))
}

private fun requireUniqueFieldNumbers(message: ResolvedProtobufMessage) {
    requireUniqueUsages(message.name, "field numbers", collectFieldNumberUsages(message))
}

private fun requireUniqueOneofNames(message: ResolvedProtobufMessage) {
    val duplicates = message.oneofs
        .groupBy { it.name }
        .filterValues { it.size > 1 }

    require(duplicates.isEmpty()) {
        val names = duplicates.keys.sorted().joinToString(", ")
        "Message '${message.name}' has duplicate oneof names: $names"
    }
}

private fun collectFieldNameUsages(message: ResolvedProtobufMessage): List<FieldUsage<String>> = buildList {
    message.fields.forEach { field -> add(FieldUsage(field.name, "field '${field.name}'")) }
    message.oneofs.forEach { oneof ->
        oneof.fields.forEach { field -> add(FieldUsage(field.name, "oneof '${oneof.name}' field '${field.name}'")) }
    }
}

private fun collectFieldNumberUsages(message: ResolvedProtobufMessage): List<FieldUsage<Int>> = buildList {
    message.fields.forEach { field -> add(FieldUsage(field.number, "field '${field.name}'")) }
    message.oneofs.forEach { oneof ->
        oneof.fields.forEach { field -> add(FieldUsage(field.number, "oneof '${oneof.name}' field '${field.name}'")) }
    }
}

private fun <K : Comparable<K>> requireUniqueUsages(messageName: String, label: String, usages: List<FieldUsage<K>>) {
    val duplicates = usages.groupBy(keySelector = FieldUsage<K>::key, valueTransform = FieldUsage<K>::location)
        .filterValues { it.size > 1 }

    require(duplicates.isEmpty()) {
        val details = duplicates.toSortedMap()
            .entries
            .joinToString("; ") { (duplicateKey, locations) -> "$duplicateKey (${locations.joinToString()})" }
        "Duplicate $label in message '$messageName': $details"
    }
}

private data class FieldUsage<K>(val key: K, val location: String)
