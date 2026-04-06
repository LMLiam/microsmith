package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetField
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType

data class ResolvedDotnetAspEndpoint(
    val method: DotnetAspHttpMethod,
    val route: String,
    val routePlaceholders: List<String>,
    val operationName: String,
    val bindings: ResolvedDotnetAspEndpointBindings,
    val responses: List<ResolvedDotnetAspResponse>,
) {
    init {
        DotnetField(operationName, DotnetFieldType.String)
    }
}
