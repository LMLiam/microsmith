package io.github.lmliam.microsmith.dsl.services.dotnet.core

/**
 * Canonical .NET service-local model declaration.
 */
data class DotnetModel(
    val name: String,
    val fields: List<DotnetField>,
) {
    init {
        validateDotnetIdentifier(name, "Model name")
    }
}
