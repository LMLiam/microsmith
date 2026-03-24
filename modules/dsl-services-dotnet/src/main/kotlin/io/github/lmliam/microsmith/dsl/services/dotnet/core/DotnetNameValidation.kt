package io.github.lmliam.microsmith.dsl.services.dotnet.core

private val DOTNET_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")
private val DOTNET_QUALIFIED_IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)*")

internal fun validateDotnetIdentifier(value: String, label: String): String {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(DOTNET_IDENTIFIER.matches(normalized)) {
        "$label is not a valid .NET identifier: '$value'"
    }

    return normalized
}

internal fun validateDotnetQualifiedIdentifier(value: String, label: String): String {
    val normalized = value.trim()
    require(normalized.isNotBlank()) { "$label cannot be blank." }
    require(DOTNET_QUALIFIED_IDENTIFIER.matches(normalized)) {
        "$label is not a valid .NET qualified identifier: '$value'"
    }

    return normalized
}
