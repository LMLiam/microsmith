package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class DotnetAspEndpoint(
    val method: DotnetAspHttpMethod,
    val path: String = "",
    val operationName: String,
    val bindings: DotnetAspEndpointBindings = DotnetAspEndpointBindings(),
    val responses: List<DotnetAspResponse>,
) {
    init {
        DotnetField(operationName, DotnetFieldType.String)
        require(responses.isNotEmpty()) {
            "ASP.NET endpoint '$operationName' must declare at least one response."
        }
    }
}
