package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRoute
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue

internal class DotnetAspEndpointContentResolver {
    private val bindingsResolver = DotnetAspEndpointBindingsResolver()
    private val responseResolver = DotnetAspResponseResolver()

    fun resolve(
        context: DotnetAspOperationContext,
        endpoint: DotnetAspEndpoint,
        route: ResolvedDotnetAspRoute,
        models: Map<String, DotnetModel>,
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspEndpoint> =
        Either.zipOrAccumulate(
            bindingsResolver.resolve(context, endpoint, route, models),
            responseResolver.resolve(context, endpoint, models),
        ) { bindings, responses ->
            ResolvedDotnetAspEndpoint(
                endpoint.method,
                route.path,
                route.placeholders,
                endpoint.operationName,
                bindings,
                responses,
            )
        }
}
