package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.route.DotnetAspRouteGroup

internal class DotnetAspRouteGroupBuilder(private val path: String) : DotnetAspRouteTreeBuilder() {

    fun build() = DotnetAspRouteGroup(
        path = path,
        groups = groups.toList(),
        endpoints = endpoints.toList(),
    )
}
