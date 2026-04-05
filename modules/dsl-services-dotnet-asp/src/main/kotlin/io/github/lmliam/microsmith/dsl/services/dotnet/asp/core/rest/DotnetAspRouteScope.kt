package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface DotnetAspRouteScope {
    operator fun String.invoke(block: DotnetAspRouteScope.() -> Unit)

    fun get(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun get(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun post(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun post(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun put(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun put(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun patch(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun patch(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun delete(operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})

    fun delete(path: String, operationName: String, block: DotnetAspEndpointScope.() -> Unit = {})
}
