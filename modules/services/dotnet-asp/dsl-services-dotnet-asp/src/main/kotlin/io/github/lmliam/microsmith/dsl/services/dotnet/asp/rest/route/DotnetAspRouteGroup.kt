package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.route

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint

public data class DotnetAspRouteGroup(
    public val path: String,
    public val groups: List<DotnetAspRouteGroup> = emptyList(),
    public val endpoints: List<DotnetAspEndpoint> = emptyList(),
) {
    init {
        require(path.isNotBlank()) { "ASP.NET route group path cannot be blank." }
    }
}
