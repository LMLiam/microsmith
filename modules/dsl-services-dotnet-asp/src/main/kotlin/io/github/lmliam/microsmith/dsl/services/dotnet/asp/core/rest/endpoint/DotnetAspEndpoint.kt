package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.response.DotnetAspResponse
import io.github.lmliam.microsmith.dsl.services.dotnet.core.support.validateDotnetIdentifier

data class DotnetAspEndpoint(
    val method: DotnetAspHttpMethod,
    val path: String,
    val operationName: String,
    val bindings: DotnetAspEndpointBindings = DotnetAspEndpointBindings(),
    val responses: List<DotnetAspResponse>,
) {
    init {
        validateDotnetIdentifier(operationName, "ASP.NET operation name")
        require(responses.isNotEmpty()) {
            "ASP.NET endpoint '$operationName' must declare at least one response."
        }
    }
}
