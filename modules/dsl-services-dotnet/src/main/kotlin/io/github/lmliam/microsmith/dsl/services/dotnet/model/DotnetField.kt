package io.github.lmliam.microsmith.dsl.services.dotnet.model

import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

/**
 * A single .NET model field.
 */
data class DotnetField(val name: String, val type: DotnetFieldType) {
    init {
        validateDotnetIdentifier(name, "Field name")
    }
}
