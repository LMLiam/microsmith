package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.service

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspEndpoint
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspEndpointBuilder
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspEndpointScope
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.endpoint.DotnetAspHttpMethod
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.route.DotnetAspRouteGroup
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.route.DotnetAspRouteScope

internal open class DotnetAspRouteTreeBuilder : DotnetAspRouteScope {
    protected val groups = mutableListOf<DotnetAspRouteGroup>()
    protected val endpoints = mutableListOf<DotnetAspEndpoint>()

    override fun String.invoke(block: DotnetAspRouteScope.() -> Unit) {
        groups += DotnetAspRouteGroupBuilder(this).apply(block).build()
    }

    override fun get(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.GET, operationName, block)
    }

    override fun get(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.GET, path, operationName, block)
    }

    override fun post(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.POST, operationName, block)
    }

    override fun post(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.POST, path, operationName, block)
    }

    override fun put(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PUT, operationName, block)
    }

    override fun put(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PUT, path, operationName, block)
    }

    override fun patch(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PATCH, operationName, block)
    }

    override fun patch(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PATCH, path, operationName, block)
    }

    override fun delete(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.DELETE, operationName, block)
    }

    override fun delete(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.DELETE, path, operationName, block)
    }

    private fun buildEndpoint(
        method: DotnetAspHttpMethod,
        operationName: String,
        block: DotnetAspEndpointScope.() -> Unit,
    ): DotnetAspEndpoint {
        val builder = DotnetAspEndpointBuilder(method, operationName)
        builder.block()
        return builder.build()
    }

    private fun buildEndpoint(
        method: DotnetAspHttpMethod,
        path: String,
        operationName: String,
        block: DotnetAspEndpointScope.() -> Unit,
    ): DotnetAspEndpoint {
        val builder = DotnetAspEndpointBuilder(method, path, operationName)
        builder.block()
        return builder.build()
    }
}
