package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

data class DotnetAspRequestField(
    val name: String,
    val type: DotnetFieldType,
    val optional: Boolean = false,
    val defaultValue: DotnetAspDefaultValue? = null,
) {
    init {
        validateDotnetIdentifier(name, "ASP.NET request field name")
    }
}
