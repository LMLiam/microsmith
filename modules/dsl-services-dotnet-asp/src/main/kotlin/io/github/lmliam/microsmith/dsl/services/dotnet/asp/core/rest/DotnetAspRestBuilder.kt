package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

internal class DotnetAspRestBuilder : DotnetAspRestScope {
    private val groups = mutableListOf<DotnetAspRouteGroup>()
    private val endpoints = mutableListOf<DotnetAspEndpoint>()

    override fun String.invoke(block: DotnetAspRouteScope.() -> Unit) {
        val builder = DotnetAspRouteGroupBuilder(this).apply(block)
        groups += builder.build()
    }

    override fun get(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.GET, "", operationName, block)
    }

    override fun get(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.GET, path, operationName, block)
    }

    override fun post(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.POST, "", operationName, block)
    }

    override fun post(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.POST, path, operationName, block)
    }

    override fun put(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PUT, "", operationName, block)
    }

    override fun put(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PUT, path, operationName, block)
    }

    override fun patch(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PATCH, "", operationName, block)
    }

    override fun patch(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PATCH, path, operationName, block)
    }

    override fun delete(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.DELETE, "", operationName, block)
    }

    override fun delete(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.DELETE, path, operationName, block)
    }

    fun build() = DotnetAspRest(
        groups = groups.toList(),
        endpoints = endpoints.toList(),
    )

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

private class DotnetAspRouteGroupBuilder(
    private val path: String,
) : DotnetAspRouteScope {
    private val groups = mutableListOf<DotnetAspRouteGroup>()
    private val endpoints = mutableListOf<DotnetAspEndpoint>()

    override fun String.invoke(block: DotnetAspRouteScope.() -> Unit) {
        val builder = DotnetAspRouteGroupBuilder(this).apply(block)
        groups += builder.build()
    }

    override fun get(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.GET, "", operationName, block)
    }

    override fun get(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.GET, path, operationName, block)
    }

    override fun post(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.POST, "", operationName, block)
    }

    override fun post(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.POST, path, operationName, block)
    }

    override fun put(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PUT, "", operationName, block)
    }

    override fun put(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PUT, path, operationName, block)
    }

    override fun patch(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PATCH, "", operationName, block)
    }

    override fun patch(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.PATCH, path, operationName, block)
    }

    override fun delete(operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.DELETE, "", operationName, block)
    }

    override fun delete(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit) {
        endpoints += buildEndpoint(DotnetAspHttpMethod.DELETE, path, operationName, block)
    }

    fun build() = DotnetAspRouteGroup(
        path = path,
        groups = groups.toList(),
        endpoints = endpoints.toList(),
    )

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
