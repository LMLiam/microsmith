package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspHttpMethod

data class ResolvedDotnetAspEndpoint(
    val method: DotnetAspHttpMethod,
    val route: String,
    val routePlaceholders: List<String>,
    val operationName: String,
    val bindings: ResolvedDotnetAspEndpointBindings,
    val responses: List<ResolvedDotnetAspResponse>,
)
