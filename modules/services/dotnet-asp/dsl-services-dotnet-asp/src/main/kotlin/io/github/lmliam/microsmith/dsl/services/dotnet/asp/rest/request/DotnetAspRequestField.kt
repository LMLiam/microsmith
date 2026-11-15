package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

public data class DotnetAspRequestField(
    public val name: String,
    public val type: DotnetFieldType,
    public val optional: Boolean = false,
    public val defaultValue: DotnetAspDefaultValue? = null,
) {
    init {
        validateDotnetIdentifier(name, "ASP.NET request field name")
    }
}
