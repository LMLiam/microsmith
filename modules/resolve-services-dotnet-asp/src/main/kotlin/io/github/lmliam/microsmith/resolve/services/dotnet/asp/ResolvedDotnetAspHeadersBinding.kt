package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

data class ResolvedDotnetAspHeadersBinding(val name: String, val headers: List<ResolvedDotnetAspHeaderField>) {
    init {
        validateDotnetIdentifier(name, "ASP.NET headers binding name")
    }
}
