package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

public data class DotnetAspHeaderField(public val name: String, public val headerName: String) {
    init {
        validateDotnetIdentifier(name, "ASP.NET header field name")
        require(headerName.isNotBlank()) { "ASP.NET header name cannot be blank." }
    }
}
