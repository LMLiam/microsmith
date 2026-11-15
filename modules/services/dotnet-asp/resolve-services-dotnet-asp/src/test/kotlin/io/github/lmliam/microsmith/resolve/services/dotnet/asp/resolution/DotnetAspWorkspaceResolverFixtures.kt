package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing.DotnetAspRestTreeResolver
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing.DotnetAspRouteResolver
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing.DotnetAspRouteSegmentParser
import io.github.lmliam.microsmith.resolve.services.dotnet.resolution.DotnetWorkspaceResolver

internal fun dotnetAspWorkspaceResolver(): DotnetAspWorkspaceResolver {
    val modelResolver = DotnetAspModelResolver()
    val bindingsResolver = DotnetAspEndpointBindingsResolver(DotnetAspBindingResolver(), modelResolver)
    val responseResolver = DotnetAspResponseResolver(modelResolver)
    val contentResolver = DotnetAspEndpointContentResolver(bindingsResolver, responseResolver)
    val routeResolver = DotnetAspRouteResolver(DotnetAspRouteSegmentParser())
    val endpointResolver = DotnetAspEndpointResolver(routeResolver, contentResolver)
    val treeResolver = DotnetAspRestTreeResolver(endpointResolver, routeResolver)

    return DotnetAspWorkspaceResolver(
        dotnetWorkspaceResolver = DotnetWorkspaceResolver(),
        restResolver = DotnetAspRestResolver(treeResolver),
        portAllocator = DotnetAspPortAllocator(),
    )
}
