package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

/** Canonical .NET service-local model declaration. */
public data class DotnetModel(public val name: String, public val fields: List<DotnetField>) {
    init {
        validateDotnetIdentifier(name, "Model name")
    }
}
