package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.route.DotnetAspRouteGroup

data class DotnetAspRest(
    val groups: List<DotnetAspRouteGroup> = emptyList(),
    val endpoints: List<DotnetAspEndpoint> = emptyList(),
) {
    fun merge(other: DotnetAspRest) = DotnetAspRest(
        groups = groups + other.groups,
        endpoints = endpoints + other.endpoints,
    )
}
