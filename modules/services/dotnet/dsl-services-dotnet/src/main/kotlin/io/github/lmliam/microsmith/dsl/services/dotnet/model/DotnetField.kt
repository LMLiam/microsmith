package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

/** A single .NET model field. */
public data class DotnetField(public val name: String, public val type: DotnetFieldType) {
    init {
        validateDotnetIdentifier(name, "Field name")
    }
}
