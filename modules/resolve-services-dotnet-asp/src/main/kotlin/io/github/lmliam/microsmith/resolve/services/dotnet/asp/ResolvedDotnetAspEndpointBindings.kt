package io.github.lmliam.microsmith.resolve.services.dotnet.asp

data class ResolvedDotnetAspEndpointBindings(
    val path: ResolvedDotnetAspRequestBinding? = null,
    val query: ResolvedDotnetAspRequestBinding? = null,
    val headers: ResolvedDotnetAspHeadersBinding? = null,
    val body: ResolvedDotnetAspModel? = null,
)
