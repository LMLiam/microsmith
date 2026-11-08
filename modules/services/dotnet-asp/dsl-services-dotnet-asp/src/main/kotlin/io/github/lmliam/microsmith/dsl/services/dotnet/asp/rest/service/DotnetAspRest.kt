package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.route.DotnetAspRouteGroup

data class DotnetAspRest(
    val groups: List<DotnetAspRouteGroup> = emptyList(),
    val endpoints: List<DotnetAspEndpoint> = emptyList(),
)
