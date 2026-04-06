package io.github.lmliam.microsmith.artifact.schemas.protobuf.emission

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Message
import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.ProtobufNameValidation

internal object MessageEmissionValidator {
    fun validate(message: Message) {
        ProtobufNameValidation.requireIdentifier(message.name, "Message name")

        message.fields.forEach(FieldEmissionValidator::validate)
        message.oneofs.forEach(FieldEmissionValidator::validate)
        message.reserved.forEach(ReservedEmissionValidator::validate)

        requireUniqueFieldNames(message)
        requireUniqueFieldNumbers(message)
        requireUniqueOneofNames(message)
        ReservedUsageCollisionValidator.validate(message)
    }

    private fun requireUniqueFieldNames(message: Message) {
        requireUniqueUsages(
            messageName = message.name,
            label = "field names",
            usages = collectFieldNameUsages(message),
        )
    }

    private fun requireUniqueFieldNumbers(message: Message) {
        requireUniqueUsages(
            messageName = message.name,
            label = "field numbers",
            usages = collectFieldNumberUsages(message),
        )
    }

    private fun requireUniqueOneofNames(message: Message) {
        val duplicates = message.oneofs.groupBy { it.name }.filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val names = duplicates.keys.sorted().joinToString(", ")
            "Message '${message.name}' has duplicate oneof names: $names"
        }
    }

    private fun collectFieldNameUsages(message: Message): List<Pair<String, String>> =
        collectFieldUsages(message, keySelector = { it.name })

    private fun collectFieldNumberUsages(message: Message): List<Pair<Int, String>> =
        collectFieldUsages(message, keySelector = { it.index })

    private fun <Key : Comparable<Key>> requireUniqueUsages(
        messageName: String,
        label: String,
        usages: List<Pair<Key, String>>,
    ) {
        val duplicates =
            usages
                .groupBy(keySelector = Pair<Key, String>::first, valueTransform = Pair<Key, String>::second)
                .filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val details =
                duplicates
                    .toSortedMap()
                    .entries
                    .joinToString("; ") { (duplicateKey, locations) ->
                        "$duplicateKey (${locations.joinToString()})"
                    }
            "Duplicate $label in message '$messageName': $details"
        }
    }

    private fun <Key> collectFieldUsages(message: Message, keySelector: (Field) -> Key): List<Pair<Key, String>> =
        buildList {
            message.fields.forEach { add(keySelector(it) to "field '${it.name}'") }
            message.oneofs.forEach { oneof ->
                oneof.fields.forEach { field ->
                    add(keySelector(field) to "oneof '${oneof.name}' field '${field.name}'")
                }
            }
        }
}
