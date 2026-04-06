package io.github.lmliam.microsmith.dsl.services.dotnet.core.model

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

/**
 * A single .NET model field.
 */
data class DotnetField(val name: String, val type: DotnetFieldType) {
    init {
        validateDotnetIdentifier(name, "Field name")
    }
}
