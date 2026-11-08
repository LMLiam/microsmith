package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.EitherNel
import arrow.core.flatMap
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing.DotnetAspRouteFragment
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing.DotnetAspRouteResolver

internal class DotnetAspEndpointResolver {
    private val routeResolver = DotnetAspRouteResolver()
    private val contentResolver = DotnetAspEndpointContentResolver()

    fun resolve(
        serviceName: String,
        endpoint: DotnetAspEndpoint,
        parentRoute: DotnetAspRouteFragment,
        models: Map<String, DotnetModel>,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspEndpoint> {
        val context = DotnetAspOperationContext(serviceName, endpoint.operationName)
        return routeResolver
            .parseDeclaredRoute(
                route = endpoint.path,
                kind = DotnetAspResolutionIssue.RouteDeclarationKind.ENDPOINT,
                allowEmpty = true,
            )
            .flatMap { endpointRoute -> routeResolver.resolve(context, fragment = parentRoute + endpointRoute) }
            .flatMap { route -> contentResolver.resolve(context, endpoint, route, models) }
    }
}
