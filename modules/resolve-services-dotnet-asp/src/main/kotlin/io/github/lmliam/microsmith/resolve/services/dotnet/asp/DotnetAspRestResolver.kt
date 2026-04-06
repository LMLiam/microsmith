package io.github.lmliam.microsmith.resolve.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.DotnetAspRest
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.DotnetAspRouteGroup
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetModel

internal class DotnetAspRestResolver {
    private val bindingResolver = DotnetAspBindingResolver()
    private val routeResolver = DotnetAspRouteResolver()

    fun resolve(serviceName: String, models: Map<String, DotnetModel>, rest: DotnetAspRest?): ResolvedDotnetAspRest {
        if (rest == null) {
            return ResolvedDotnetAspRest.empty()
        }

        val endpoints = mutableListOf<ResolvedDotnetAspEndpoint>()
        rest.endpoints.forEach { endpoint ->
            endpoints += resolveEndpoint(serviceName, endpoint, emptyList(), models)
        }
        rest.groups.forEach { group ->
            endpoints += resolveGroup(serviceName, group, emptyList(), models)
        }

        val operationCollisions =
            endpoints
                .groupBy(ResolvedDotnetAspEndpoint::operationName)
                .filterValues { it.size > 1 }
                .keys
                .sorted()
        require(operationCollisions.isEmpty()) {
            "ASP.NET service '$serviceName' declares duplicate operation names: " +
                operationCollisions.joinToString(", ") + "."
        }
        val routeCollisions =
            endpoints
                .groupBy { "${it.method} ${it.route}" }
                .filterValues { it.size > 1 }
                .keys
                .sorted()
        require(routeCollisions.isEmpty()) {
            "ASP.NET service '$serviceName' declares duplicate REST endpoints: " +
                routeCollisions.joinToString(", ") + "."
        }

        val sortedEndpoints =
            endpoints.sortedWith(
                compareBy(
                    ResolvedDotnetAspEndpoint::route,
                    ResolvedDotnetAspEndpoint::operationName,
                ),
            )
        return ResolvedDotnetAspRest(sortedEndpoints)
    }

    private fun resolveGroup(
        serviceName: String,
        group: DotnetAspRouteGroup,
        parentSegments: List<String>,
        models: Map<String, DotnetModel>,
    ): List<ResolvedDotnetAspEndpoint> {
        val currentSegments = parentSegments + routeResolver.parseDeclaredRoute(group.path, "Route group")
        val endpoints = mutableListOf<ResolvedDotnetAspEndpoint>()
        group.endpoints.forEach { endpoint ->
            endpoints += resolveEndpoint(serviceName, endpoint, currentSegments, models)
        }
        group.groups.forEach { child ->
            endpoints += resolveGroup(serviceName, child, currentSegments, models)
        }
        return endpoints
    }

    private fun resolveEndpoint(
        serviceName: String,
        endpoint: DotnetAspEndpoint,
        parentSegments: List<String>,
        models: Map<String, DotnetModel>,
    ): ResolvedDotnetAspEndpoint {
        val routeSegments =
            parentSegments + routeResolver.parseDeclaredRoute(
                endpoint.path,
                "Endpoint route",
                allowEmpty = true,
            )
        val route = routeResolver.normalizeRoute(routeSegments)
        val placeholders = routeResolver.extractRoutePlaceholders(routeSegments, route)

        require(placeholders.isEmpty() == (endpoint.bindings.path == null)) {
            if (placeholders.isEmpty()) {
                "ASP.NET endpoint '${endpoint.operationName}' in service '$serviceName' " +
                    "declares a path binding but route '$route' has no placeholders."
            } else {
                "ASP.NET endpoint '${endpoint.operationName}' in service '$serviceName' " +
                    "must declare a path binding for route '$route'."
            }
        }
        val pathBinding =
            endpoint.bindings.path?.let {
                bindingResolver.resolvePathBinding(serviceName, endpoint, placeholders, it, models)
            }

        return ResolvedDotnetAspEndpoint(
            method = endpoint.method,
            route = route,
            routePlaceholders = placeholders,
            operationName = endpoint.operationName,
            bindings = ResolvedDotnetAspEndpointBindings(
                path = pathBinding,
                query =
                endpoint.bindings.query?.let {
                    bindingResolver.resolveRequestBinding(serviceName, endpoint.operationName, it, models)
                },
                headers = endpoint.bindings.headers?.let(bindingResolver::resolveHeadersBinding),
                body =
                endpoint.bindings.body?.let {
                    bindingResolver.resolveModelReference(serviceName, endpoint.operationName, models, it)
                },
            ),
            responses = endpoint.responses.map { response ->
                ResolvedDotnetAspResponse(
                    statusCode = response.statusCode,
                    model =
                    bindingResolver.resolveModelReference(
                        serviceName,
                        endpoint.operationName,
                        models,
                        response.model,
                    ),
                    headers = response.headers.map { ResolvedDotnetAspResponseHeader(it.name) },
                )
            },
        )
    }
}
