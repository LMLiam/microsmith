package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

data class ResolvedDotnetAspRequestBinding(
    val name: String,
    val fields: List<ResolvedDotnetAspRequestField>,
) {
    init {
        validateDotnetIdentifier(name, "ASP.NET request binding name")
    }
}
