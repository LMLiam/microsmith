package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpointBindings
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspResponse
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
    ): EitherNel<DotnetAspResolutionIssue, ResolvedDotnetAspEndpoint> {
        val issues = mutableListOf<DotnetAspResolutionIssue>()

        var bindings: ResolvedDotnetAspEndpointBindings? = null
        var responses: List<ResolvedDotnetAspResponse>? = null

        bindingsResolver
            .resolve(context, endpoint, route, models)
            .fold(
                ifLeft = issues::addAll,
                ifRight = { bindings = it },
            )

        responseResolver
            .resolve(context, endpoint, models)
            .fold(
                ifLeft = issues::addAll,
                ifRight = { responses = it },
            )

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        if (accumulatedIssues != null) {
            return Either.Left(accumulatedIssues)
        }

        return Either.Right(
            ResolvedDotnetAspEndpoint(
                endpoint.method,
                route.path,
                route.placeholders,
                endpoint.operationName,
                bindings = checkNotNull(bindings),
                responses = checkNotNull(responses),
            )
        )
    }
}
