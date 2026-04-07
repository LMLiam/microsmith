package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service

internal class DotnetAspRestBuilder :
    DotnetAspRouteTreeBuilder(),
    DotnetAspRestScope {

    fun build() = DotnetAspRest(
        groups = groups.toList(),
        endpoints = endpoints.toList(),
    )
}
