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
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspGenerationResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.diagnostics.DotnetAspResolutionIssue
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspResultBaseTypeName
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.naming.dotnetAspResultVariantTypeName
import io.github.lmliam.microsmith.resolve.services.dotnet.asp.routing.DotnetAspRestTreeResolver

internal class DotnetAspRestResolver {
    private val treeResolver = DotnetAspRestTreeResolver()

    fun resolve(
        serviceName: String,
        models: Map<String, DotnetModel>,
        rest: DotnetAspRest?,
    ): EitherNel<DotnetResolutionIssue, ResolvedDotnetAspRest> {
        if (rest == null) return Either.Right(ResolvedDotnetAspRest.empty())

        return treeResolver.resolve(serviceName, models, rest).flatMap { endpoints ->
            resolveEndpointSet(serviceName, models, endpoints)
        }
    }

    private fun resolveEndpointSet(
        serviceName: String,
        models: Map<String, DotnetModel>,
        endpoints: List<ResolvedDotnetAspEndpoint>,
    ): EitherNel<DotnetResolutionIssue, ResolvedDotnetAspRest> {
        val issues =
            collisionIssues(serviceName, endpoints) + contractTypeCollisionIssues(serviceName, models, endpoints)

        val accumulatedIssues = issues.toNonEmptyListOrNull()

        if (accumulatedIssues != null) return Either.Left(accumulatedIssues)

        return Either.Right(
            ResolvedDotnetAspRest(
                endpoints.sortedWith(
                    compareBy(
                        ResolvedDotnetAspEndpoint::route,
                        ResolvedDotnetAspEndpoint::operationName,
                    )
                )
            )
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
                )
            )
            .forEach { (method, route) ->
                add(DotnetAspResolutionIssue.DuplicateRestEndpoint(serviceName, method, route))
            }
    }

    private fun contractTypeCollisionIssues(
        serviceName: String,
        models: Map<String, DotnetModel>,
        endpoints: List<ResolvedDotnetAspEndpoint>,
    ): List<DotnetAspResolutionIssue> {
        val owners = mutableListOf<Pair<String, String>>()

        models.keys.forEach { name -> owners += name to "shared model '$name'" }
        endpoints.distinctBy(ResolvedDotnetAspEndpoint::operationName).forEach { endpoint ->
            owners +=
                dotnetAspResultBaseTypeName(endpoint.operationName) to
                    "result base for operation '${endpoint.operationName}'"
            endpoint.responses.forEach { response ->
                owners +=
                    dotnetAspResultVariantTypeName(endpoint.operationName, response.statusCode) to
                        "response result for operation '${endpoint.operationName}' status ${response.statusCode}"
            }
        }

        return owners
            .groupBy({ (typeName, _) -> typeName }, { (_, owner) -> owner })
            .filterValues { it.size > 1 }
            .toSortedMap()
            .map { (typeName, typeOwners) ->
                DotnetAspGenerationResolutionIssue.ContractTypeCollision(serviceName, typeName, typeOwners.sorted())
            }
    }
}
