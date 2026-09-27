package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

data class DotnetAspRequestBinding(val name: String, val fields: List<DotnetAspRequestField>) {
    init {
        require(fields.isNotEmpty()) {
            "ASP.NET request binding '$name' must declare at least one field."
        }
        validateDotnetIdentifier(name, "ASP.NET request binding name")
    }
}
