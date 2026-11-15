package io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.route

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.services.dotnet.asp.rest.endpoint.DotnetAspEndpointScope

@MicrosmithDsl
public interface DotnetAspRouteScope {
    public operator fun String.invoke(block: DotnetAspRouteScope.() -> Unit)

    public fun get(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun get(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun post(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun post(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun put(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun put(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun patch(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun patch(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun delete(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    public fun delete(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})
}
