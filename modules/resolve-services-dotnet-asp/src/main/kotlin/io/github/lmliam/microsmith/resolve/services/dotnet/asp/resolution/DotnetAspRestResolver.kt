package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.flatMap
import arrow.core.toNonEmptyListOrNull
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.service.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetModel
import io.github.lmliam.microsmith.resolve.services.dotnet.DotnetResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspEndpoint
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.ResolvedDotnetAspRest
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing.DotnetAspRestTreeResolver

internal class DotnetAspRestResolver {
    private val treeResolver = DotnetAspRestTreeResolver()

    fun resolve(
        serviceName: String,
        models: Map<String, DotnetModel>,
        rest: DotnetAspRest?,
    ): EitherNel<DotnetResolutionIssue, ResolvedDotnetAspRest> {
        if (rest == null) return Either.Right(ResolvedDotnetAspRest.empty())

        return treeResolver.resolve(serviceName, models, rest)
            .flatMap { endpoints -> resolveEndpointSet(serviceName, endpoints) }
    }

    private fun resolveEndpointSet(
        serviceName: String,
        endpoints: List<ResolvedDotnetAspEndpoint>,
    ): EitherNel<DotnetResolutionIssue, ResolvedDotnetAspRest> {
        val issues = collisionIssues(serviceName, endpoints)

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        if (accumulatedIssues != null) return Either.Left(accumulatedIssues)

        return Either.Right(
            ResolvedDotnetAspRest(
                endpoints.sortedWith(
                    compareBy(
                        ResolvedDotnetAspEndpoint::route,
                        ResolvedDotnetAspEndpoint::operationName,
                    ),
                ),
            ),
        )
    }

    private fun collisionIssues(
        serviceName: String,
        endpoints: List<ResolvedDotnetAspEndpoint>,
    ): List<DotnetAspResolutionIssue> = buildList {
        endpoints
            .groupBy(ResolvedDotnetAspEndpoint::operationName)
            .filterValues { it.size > 1 }
            .keys
            .sorted()
            .forEach { operationName ->
                add(DotnetAspResolutionIssue.DuplicateOperationName(serviceName, operationName))
            }

        endpoints
            .groupBy { it.method to it.route }
            .filterValues { it.size > 1 }
            .keys
            .sortedWith(
                compareBy(
                    { it.first.name },
                    { it.second },
                ),
            )
            .forEach { (method, route) ->
                add(DotnetAspResolutionIssue.DuplicateRestEndpoint(serviceName, method, route))
            }
    }
}
