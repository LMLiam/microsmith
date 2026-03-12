package me.liam.microsmith.gen.schemas.protobuf.emission

import me.liam.microsmith.dsl.schemas.protobuf.types.Message
import me.liam.microsmith.gen.schemas.protobuf.names.ProtobufNameValidation

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
        val duplicates =
            collectFieldNameUsages(message)
                .groupBy(keySelector = Pair<String, String>::first, valueTransform = Pair<String, String>::second)
                .filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val details =
                duplicates
                    .toSortedMap()
                    .entries
                    .joinToString("; ") { (duplicateName, locations) ->
                        "$duplicateName (${locations.joinToString()})"
                    }
            "Duplicate field names in message '${message.name}': $details"
        }
    }

    private fun requireUniqueFieldNumbers(message: Message) {
        val duplicates =
            collectFieldNumberUsages(message)
                .groupBy(keySelector = Pair<Int, String>::first, valueTransform = Pair<Int, String>::second)
                .filterValues { it.size > 1 }

        require(duplicates.isEmpty()) {
            val details =
                duplicates
                    .toSortedMap()
                    .entries
                    .joinToString("; ") { (duplicateNumber, locations) ->
                        "$duplicateNumber (${locations.joinToString()})"
                    }
            "Duplicate field numbers in message '${message.name}': $details"
        }
    }

    private fun requireUniqueOneofNames(message: Message) {
        val duplicates = message.oneofs.groupBy { it.name }.filterValues { it.size > 1 }
        require(duplicates.isEmpty()) {
            val names = duplicates.keys.sorted().joinToString(", ")
            "Message '${message.name}' has duplicate oneof names: $names"
        }
    }

    private fun collectFieldNameUsages(message: Message): List<Pair<String, String>> = buildList {
        message.fields.forEach { add(it.name to "field '${it.name}'") }
        message.oneofs.forEach { oneof ->
            oneof.fields.forEach { field ->
                add(field.name to "oneof '${oneof.name}' field '${field.name}'")
            }
        }
    }

    private fun collectFieldNumberUsages(message: Message): List<Pair<Int, String>> = buildList {
        message.fields.forEach { add(it.index to "field '${it.name}'") }
        message.oneofs.forEach { oneof ->
            oneof.fields.forEach { field ->
                add(field.index to "oneof '${oneof.name}' field '${field.name}'")
            }
        }
    }
}
