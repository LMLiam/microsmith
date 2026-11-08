package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service

internal class DotnetAspRestBuilder : DotnetAspRouteTreeBuilder(), DotnetAspRestScope {

    fun build() =
        DotnetAspRest(
            groups = groups.toList(),
            endpoints = endpoints.toList(),
        )
}
