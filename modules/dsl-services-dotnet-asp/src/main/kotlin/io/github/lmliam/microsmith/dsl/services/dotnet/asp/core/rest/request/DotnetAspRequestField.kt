package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

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
