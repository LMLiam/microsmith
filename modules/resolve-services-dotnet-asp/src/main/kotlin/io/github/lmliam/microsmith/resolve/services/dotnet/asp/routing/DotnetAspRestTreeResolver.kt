package io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing
import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.route.DotnetAspRouteGroup
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution.DotnetAspEndpointResolver

internal class DotnetAspRestTreeResolver {
    private val endpointResolver = DotnetAspEndpointResolver()
    private val routeResolver = DotnetAspRouteResolver()

    fun resolve(
        serviceName: String,
        models: Map<String, DotnetModel>,
        rest: DotnetAspRest,
    ): EitherNel<DotnetResolutionIssue, List<ResolvedDotnetAspEndpoint>> =
        resolveEntries(serviceName, models, rest.endpoints, rest.groups, DotnetAspRouteFragment.Empty)

    private fun resolveGroup(
        serviceName: String,
        models: Map<String, DotnetModel>,
        group: DotnetAspRouteGroup,
        parentRoute: DotnetAspRouteFragment,
    ): EitherNel<DotnetResolutionIssue, List<ResolvedDotnetAspEndpoint>> = routeResolver
        .parseDeclaredRoute(group.path, DotnetAspResolutionIssue.RouteDeclarationKind.GROUP)
        .fold(
            ifLeft = { Either.Left(it) },
            ifRight = { groupRoute ->
                resolveEntries(serviceName, models, group.endpoints, group.groups, parentRoute + groupRoute)
            },
        )

    private fun resolveEntries(
        serviceName: String,
        models: Map<String, DotnetModel>,
        endpoints: List<DotnetAspEndpoint>,
        groups: List<DotnetAspRouteGroup>,
        parentRoute: DotnetAspRouteFragment,
    ): EitherNel<DotnetResolutionIssue, List<ResolvedDotnetAspEndpoint>> {
        val issues = mutableListOf<DotnetResolutionIssue>()
        val resolved = mutableListOf<ResolvedDotnetAspEndpoint>()

        endpoints.forEach { endpoint ->
            endpointResolver.resolve(serviceName, endpoint, parentRoute, models)
                .fold(
                    ifLeft = issues::addAll,
                    ifRight = resolved::add,
                )
        }

        groups.forEach { group ->
            resolveGroup(serviceName, models, group, parentRoute)
                .fold(
                    ifLeft = issues::addAll,
                    ifRight = resolved::addAll,
                )
        }

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        return if (accumulatedIssues != null) {
            Either.Left(accumulatedIssues)
        } else {
            Either.Right(resolved)
        }
    }
}
