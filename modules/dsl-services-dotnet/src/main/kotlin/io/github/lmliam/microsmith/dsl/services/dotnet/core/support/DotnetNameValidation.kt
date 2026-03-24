package io.github.lmliam.microsmith.dsl.services.dotnet.core.support

internal fun validateDotnetIdentifier(value: String, label: String): String {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(isDotnetIdentifier(normalized)) {
        "$label is not a valid .NET identifier: '$value'"
    }

    return normalized
}

internal fun validateDotnetQualifiedIdentifier(value: String, label: String): String {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(isDotnetQualifiedIdentifier(normalized)) {
        "$label is not a valid .NET qualified identifier: '$value'"
    }

    return normalized
}

private fun isDotnetQualifiedIdentifier(value: String): Boolean {
    return value.split('.').all(::isDotnetIdentifier)
}

private fun isDotnetIdentifier(value: String): Boolean {
    val candidate = value.removePrefix("@")
    if (candidate.isEmpty()) {
        return false
    }

    val firstCodePoint = candidate.firstCodePoint()
    if (!firstCodePoint.isDotnetIdentifierStart()) {
        return false
    }

    return candidate.asCodePoints().drop(1).all(Int::isDotnetIdentifierPart)
}

private fun String.firstCodePoint(): Int {
    return codePointAt(0)
}

private fun String.asCodePoints(): Sequence<Int> = sequence {
    var index = 0
    while (index < length) {
        val codePoint = codePointAt(index)
        yield(codePoint)
        index += Character.charCount(codePoint)
    }
}

private fun Int.isDotnetIdentifierStart(): Boolean {
    return this == '_'.code ||
        Character.isLetter(this) ||
        Character.getType(this).toInt() == Character.LETTER_NUMBER.toInt()
}

private fun Int.isDotnetIdentifierPart(): Boolean {
    return isDotnetIdentifierStart() ||
        when (Character.getType(this).toInt()) {
            Character.NON_SPACING_MARK.toInt(),
            Character.COMBINING_SPACING_MARK.toInt(),
            Character.DECIMAL_DIGIT_NUMBER.toInt(),
            Character.CONNECTOR_PUNCTUATION.toInt(),
            Character.FORMAT.toInt(),
            -> true

            else -> false
        }
}
