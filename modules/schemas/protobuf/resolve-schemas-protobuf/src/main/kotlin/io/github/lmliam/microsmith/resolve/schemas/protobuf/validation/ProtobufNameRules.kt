package io.github.lmliam.microsmith.resolve.schemas.protobuf.validation

import io.github.lmliam.microsmith.resolve.schemas.protobuf.ResolvedProtobufRejection

internal fun findJsonNameCollisions(fieldNames: List<String>): List<ResolvedProtobufRejection> =
    fieldNames
        .distinct()
        .groupBy(::defaultJsonName)
        .filterValues { it.size > 1 }
        .map { (jsonName, names) -> ResolvedProtobufRejection.FieldJsonNameCollision(jsonName, names) }

internal fun findEnumValueNameCollisions(enumName: String, valueNames: List<String>): List<ResolvedProtobufRejection> =
    valueNames
        .distinct()
        .groupBy { enumValueKey(enumName, it) }
        .filterValues { it.size > 1 }
        .map { (_, names) -> ResolvedProtobufRejection.EnumValueNameCollision(names) }

internal fun String.underscoresToCamelCase(capitalizeFirst: Boolean): String =
    split('_').withIndex().joinToString("") { (position, word) ->
        if (position == 0 && !capitalizeFirst) word else word.replaceFirstChar(Char::uppercaseChar)
    }

private fun defaultJsonName(fieldName: String): String = fieldName.underscoresToCamelCase(capitalizeFirst = false)

private fun enumValueKey(enumName: String, valueName: String): String =
    withoutEnumPrefix(enumName, valueName).split('_').joinToString("") {
        it.lowercase().replaceFirstChar(Char::uppercaseChar)
    }

private fun withoutEnumPrefix(enumName: String, valueName: String): String {
    val prefix = enumName.filter { it != '_' }.lowercase()
    val prefixLetters = valueName.withIndex().filter { it.value != '_' }.take(prefix.length)
    val startsWithPrefix =
        prefixLetters.size == prefix.length && prefixLetters.joinToString("") { it.value.lowercase() } == prefix

    if (!startsWithPrefix) return valueName

    val remainder = valueName.substring(prefixLetters.lastOrNull()?.let { it.index + 1 } ?: 0).trimStart('_')

    return remainder.ifEmpty { valueName }
}
