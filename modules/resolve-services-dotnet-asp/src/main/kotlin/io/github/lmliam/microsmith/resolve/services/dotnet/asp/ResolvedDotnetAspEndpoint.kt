package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

data class ResolvedDotnetAspEndpoint(
    val method: DotnetAspHttpMethod,
    val route: String,
    val routePlaceholders: List<String>,
    val operationName: String,
    val bindings: ResolvedDotnetAspEndpointBindings,
    val responses: List<ResolvedDotnetAspResponse>,
) {
    init {
        validateDotnetIdentifier(operationName, "ASP.NET operation name")
    }
}
