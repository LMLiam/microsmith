package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufDeclarationContext
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference

internal class ServiceBuilder(
    private val name: String,
    private val declarationContext: ProtobufDeclarationContext,
) : ServiceScope {
    private val routeNames = mutableSetOf<String>()
    private val rpcs = mutableListOf<Rpc>()

    override fun String.invoke(block: RpcScope.() -> Any?) {
        require(isNotBlank()) { "RPC name cannot be blank." }
        require(routeNames.add(this)) { "Duplicate RPC name: $this" }
        val builder = RpcBuilder(this, declarationContext)
        builder.captureResult(builder.block())
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

    override fun request(target: String, block: RpcEndpointScope.() -> Unit) {
        setRequest(buildEndpoint(target, block))
    }

    override fun response(target: String, block: RpcEndpointScope.() -> Unit) {
        setResponse(buildEndpoint(target, block))
    }

    override fun stream(target: String): RpcEndpointMarker = RpcEndpointMarker(target = target, streaming = true)

    fun build(): Rpc = Rpc(
        name = name,
        request = requireNotNull(request) { "RPC '$name' must define a request type." },
        response = requireNotNull(response) { "RPC '$name' must define a response type." },
    )

    private fun buildEndpoint(target: String, block: RpcEndpointScope.() -> Unit): RpcEndpoint {
        val scope = RpcEndpointScopeBuilder().apply(block)
        return normalizeEndpoint(RpcEndpointMarker(target, scope.streaming), "explicit endpoint")
    }

    private fun setRequest(endpoint: RpcEndpoint) {
        require(request == null) { "RPC '$name' request already defined." }
        request = endpoint
    }

    private fun setResponse(endpoint: RpcEndpoint) {
        require(response == null) { "RPC '$name' response already defined." }
        response = endpoint
    }

    private fun normalizeEndpoint(value: Any?, label: String): RpcEndpoint = when (value) {
        is String -> RpcEndpoint(Reference(declarationContext.resolveReference(value)))
        is RpcEndpointMarker -> RpcEndpoint(Reference(declarationContext.resolveReference(value.target)), value.streaming)
        else -> error("RPC '$name' $label must be a message name or stream(messageName).")
    }

    override fun toString(): String = name

    fun captureResult(result: Any?) {
        when (result) {
            null, Unit -> Unit
            is Pair<*, *> -> {
                setRequest(normalizeEndpoint(result.first, "request shorthand"))
                setResponse(normalizeEndpoint(result.second, "response shorthand"))
            }
            else -> error(
                "RPC '$name' block must use request/response declarations or a " +
                    "request-to-response pair shorthand.",
            )
        }
    }

    private class RpcEndpointScopeBuilder : RpcEndpointScope {
        var streaming: Boolean = false
            private set

        override fun stream() {
            require(!streaming) { "stream() already set for RPC endpoint." }
            streaming = true
        }
    }
}
