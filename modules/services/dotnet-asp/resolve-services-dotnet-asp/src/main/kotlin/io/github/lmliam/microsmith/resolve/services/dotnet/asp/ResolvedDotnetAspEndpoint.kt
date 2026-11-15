package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspHttpMethod

public data class ResolvedDotnetAspEndpoint(
    public val method: DotnetAspHttpMethod,
    public val route: String,
    public val routePlaceholders: List<String>,
    public val operationName: String,
    public val bindings: ResolvedDotnetAspEndpointBindings,
    public val responses: List<ResolvedDotnetAspResponse>,
)
