package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufDeclarationContext
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference

internal class ServiceBuilder(
    private val name: String,
    private val declarationContext: ProtobufDeclarationContext,
) : ServiceScope {
    private val routeNames = mutableSetOf<String>()
    private val rpcs = mutableListOf<Rpc>()

    override fun String.invoke(block: RpcScope.() -> Unit) {
        require(isNotBlank()) { "RPC name cannot be blank." }
        require(routeNames.add(this)) { "Duplicate RPC name: $this" }
        val builder = RpcBuilder(this, declarationContext)
        builder.block()
        rpcs += builder.build()
    }

    fun build(): Service = Service(name = name, rpcs = rpcs.toList())
}

private class RpcBuilder(
    private val name: String,
    private val declarationContext: ProtobufDeclarationContext,
) : RpcScope {
    private var request: RpcEndpoint? = null
    private var response: RpcEndpoint? = null
    private var declarationStyle: RpcDeclarationStyle? = null

    override fun request(target: String, block: RpcEndpointScope.() -> Unit) {
        useDeclarationStyle(RpcDeclarationStyle.EXPLICIT)
        setRequest(buildEndpoint(target, block))
    }

    override fun response(target: String, block: RpcEndpointScope.() -> Unit) {
        useDeclarationStyle(RpcDeclarationStyle.EXPLICIT)
        setResponse(buildEndpoint(target, block))
    }

    override fun stream(target: String): RpcEndpointMarker = RpcEndpointMarker(target = target, streaming = true)

    override fun String.to(other: String) {
        useDeclarationStyle(RpcDeclarationStyle.SHORTHAND)
        setRequest(normalizeEndpoint(this))
        setResponse(normalizeEndpoint(other))
    }

    override fun RpcEndpointMarker.to(other: RpcEndpointMarker) {
        useDeclarationStyle(RpcDeclarationStyle.SHORTHAND)
        setRequest(normalizeEndpoint(this))
        setResponse(normalizeEndpoint(other))
    }

    fun build(): Rpc = Rpc(
        name = name,
        request = requireNotNull(request) { "RPC '$name' must define a request type." },
        response = requireNotNull(response) { "RPC '$name' must define a response type." },
    )

    private fun buildEndpoint(target: String, block: RpcEndpointScope.() -> Unit): RpcEndpoint {
        val scope = RpcEndpointScopeBuilder().apply(block)
        return normalizeEndpoint(RpcEndpointMarker(target, scope.streaming))
    }

    private fun setRequest(endpoint: RpcEndpoint) {
        require(request == null) { "RPC '$name' request already defined." }
        request = endpoint
    }

    private fun setResponse(endpoint: RpcEndpoint) {
        require(response == null) { "RPC '$name' response already defined." }
        response = endpoint
    }

    private fun useDeclarationStyle(style: RpcDeclarationStyle) {
        val currentStyle = declarationStyle
        if (currentStyle == null) {
            declarationStyle = style
            return
        }

        require(currentStyle == style) {
            "RPC '$name' cannot mix explicit request/response declarations with pair shorthand."
        }
    }

    private fun normalizeEndpoint(target: String): RpcEndpoint =
        RpcEndpoint(Reference(declarationContext.resolveReference(target)))

    private fun normalizeEndpoint(target: RpcEndpointMarker): RpcEndpoint =
        RpcEndpoint(Reference(declarationContext.resolveReference(target.target)), target.streaming)

    override fun toString(): String = name

    private class RpcEndpointScopeBuilder : RpcEndpointScope {
        var streaming: Boolean = false
            private set

        override fun stream() {
            require(!streaming) { "stream() already set for RPC endpoint." }
            streaming = true
        }
    }

    private enum class RpcDeclarationStyle {
        EXPLICIT,
        SHORTHAND,
    }
}
