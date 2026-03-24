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
    if (value.isEmpty()) {
        return false
    }

    if (!value.first().isDotnetIdentifierStart()) {
        return false
    }

    return value.drop(1).all(Char::isDotnetIdentifierPart)
}

private fun Char.isDotnetIdentifierStart(): Boolean {
    return isAsciiLetter() || this == '_'
}

private fun Char.isDotnetIdentifierPart(): Boolean {
    return isAsciiLetterOrDigit() || this == '_'
}

private fun Char.isAsciiLetter(): Boolean {
    return this in 'a'..'z' || this in 'A'..'Z'
}

private fun Char.isAsciiLetterOrDigit(): Boolean {
    return isAsciiLetter() || isDigit()
}
