package io.github.lmliam.microsmith.resolve.services.dotnet.asp

public data class ResolvedDotnetAspEndpointBindings(
    public val path: ResolvedDotnetAspRequestBinding? = null,
    public val query: ResolvedDotnetAspRequestBinding? = null,
    public val headers: ResolvedDotnetAspHeadersBinding? = null,
    public val body: ResolvedDotnetAspModel? = null,
)
