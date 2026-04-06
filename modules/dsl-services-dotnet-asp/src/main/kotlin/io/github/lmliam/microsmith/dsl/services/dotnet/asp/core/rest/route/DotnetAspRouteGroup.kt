package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.route

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspEndpoint

data class DotnetAspRouteGroup(
    val path: String,
    val groups: List<DotnetAspRouteGroup> = emptyList(),
    val endpoints: List<DotnetAspEndpoint> = emptyList(),
) {
    init {
        require(path.isNotBlank()) { "ASP.NET route group path cannot be blank." }
    }
}
