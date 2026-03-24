package io.github.lmliam.microsmith.dsl.services.dotnet.core

/**
 * A single .NET model field.
 */
data class DotnetField(
    val name: String,
    val type: DotnetFieldType,
) {
    init {
        validateDotnetIdentifier(name, "Field name")
    }
}
