package io.github.lmliam.microsmith.dsl.services.dotnet.packages.support

internal fun normalizeDotnetPackagePath(value: String, label: String): List<String> {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(!normalized.startsWith('.')) { "$label cannot start with '.'." }
    require(!normalized.endsWith('.')) { "$label cannot end with '.'." }

    val segments = normalized.split('.')
    require(segments.all { it.isNotBlank() }) { "$label cannot contain empty path segments: '$value'." }
    require(segments.all(::isValidDotnetPackageSegment)) {
        "$label is not a valid .NET package identifier: '$value'"
    }

    return segments
}

internal fun validateDotnetPackageVersion(value: String, label: String): String {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(normalized.none(Char::isWhitespace)) { "$label cannot contain whitespace: '$value'" }
    require(
        normalized.all { character ->
            character.isLetterOrDigit() || character == '.' || character == '-' || character == '+' || character == '_'
        },
    ) {
        "$label is not a valid .NET package version: '$value'"
    }

    return normalized
}

private fun isValidDotnetPackageSegment(value: String): Boolean {
    if (value.isEmpty()) {
        return false
    }

    return value.all { character ->
        character.isLetterOrDigit() || character == '-' || character == '_'
    }
}
