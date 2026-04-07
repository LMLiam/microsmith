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

    private fun collectFieldNameUsages(message: Message): List<FieldUsage<String>> =
        collectFieldUsages(message, keySelector = { it.name })

    private fun collectFieldNumberUsages(message: Message): List<FieldUsage<Int>> =
        collectFieldUsages(message, keySelector = { it.index })

    private fun <K : Comparable<K>> requireUniqueUsages(
        messageName: String,
        label: String,
        usages: List<FieldUsage<K>>,
    ) {
        val duplicates =
            usages
                .groupBy(
                    keySelector = FieldUsage<K>::key,
                    valueTransform = FieldUsage<K>::location,
                )
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

    /**
     * Collects each effective field usage key together with where that key was declared in the
     * message tree so duplicate diagnostics can explain the conflicting locations.
     */
    private fun <K> collectFieldUsages(message: Message, keySelector: (Field) -> K): List<FieldUsage<K>> = buildList {
        message.fields.forEach { add(FieldUsage(keySelector(it), "field '${it.name}'")) }
        message.oneofs.forEach { oneof ->
            oneof.fields.forEach { field ->
                add(
                    FieldUsage(
                        key = keySelector(field),
                        location = "oneof '${oneof.name}' field '${field.name}'",
                    ),
                )
            }
        }
    }
}

private data class FieldUsage<K>(val key: K, val location: String)
