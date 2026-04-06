package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

data class ResolvedDotnetAspHeaderField(val name: String, val headerName: String) {
    init {
        validateDotnetIdentifier(name, "ASP.NET header field name")
    }
}
