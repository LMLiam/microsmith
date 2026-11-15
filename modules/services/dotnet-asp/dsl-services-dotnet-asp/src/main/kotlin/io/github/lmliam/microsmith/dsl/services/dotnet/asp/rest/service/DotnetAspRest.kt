package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.route.DotnetAspRouteGroup

public data class DotnetAspRest(
    public val groups: List<DotnetAspRouteGroup> = emptyList(),
    public val endpoints: List<DotnetAspEndpoint> = emptyList(),
)
