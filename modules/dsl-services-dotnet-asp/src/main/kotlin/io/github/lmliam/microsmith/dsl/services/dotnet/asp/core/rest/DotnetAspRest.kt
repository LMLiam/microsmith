package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

data class DotnetAspRest(
    val groups: List<DotnetAspRouteGroup> = emptyList(),
    val endpoints: List<DotnetAspEndpoint> = emptyList(),
) {
    fun merge(other: DotnetAspRest) = DotnetAspRest(
        groups = groups + other.groups,
        endpoints = endpoints + other.endpoints,
    )
}
