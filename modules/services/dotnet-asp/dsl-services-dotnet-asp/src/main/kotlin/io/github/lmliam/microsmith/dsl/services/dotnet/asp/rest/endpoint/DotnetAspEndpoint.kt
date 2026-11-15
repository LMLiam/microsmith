package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.response.DotnetAspResponse
import io.github.lmliam.microsmith.dsl.services.dotnet.validation.validateDotnetIdentifier

public data class DotnetAspEndpoint(
    public val method: DotnetAspHttpMethod,
    public val path: String,
    public val operationName: String,
    public val bindings: DotnetAspEndpointBindings = DotnetAspEndpointBindings(),
    public val responses: List<DotnetAspResponse>,
) {
    init {
        validateDotnetIdentifier(operationName, "ASP.NET operation name")
        require(responses.isNotEmpty()) { "ASP.NET endpoint '$operationName' must declare at least one response." }
    }
}
