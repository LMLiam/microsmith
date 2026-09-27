package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufDeclarationContext
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef

internal class RpcBuilder(private val name: String, declarationContext: ProtobufDeclarationContext) : RpcScope {
    private val endpoints = RpcEndpointFactory(declarationContext)

    private var request: RpcEndpoint? = null
    private var response: RpcEndpoint? = null
    private var declarationStyle: RpcDeclarationStyle? = null

    override fun request(target: String, block: RpcEndpointScope.() -> Unit) =
        setExplicitRequest(endpoints.build(target, block))

    override fun request(target: MessageRef, block: RpcEndpointScope.() -> Unit) =
        setExplicitRequest(endpoints.build(target, block))

    override fun response(target: String, block: RpcEndpointScope.() -> Unit) =
        setExplicitResponse(endpoints.build(target, block))

    override fun response(target: MessageRef, block: RpcEndpointScope.() -> Unit) =
        setExplicitResponse(endpoints.build(target, block))

    override fun stream(target: String): RpcEndpointMarker = endpoints.stream(target)

    override fun stream(target: MessageRef): RpcEndpointMarker = endpoints.stream(target)

    override fun String.to(other: String) = useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun String.to(other: MessageRef) = useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun String.to(other: RpcEndpointMarker) = useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun MessageRef.to(other: String) = useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun MessageRef.to(other: MessageRef) = useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun MessageRef.to(other: RpcEndpointMarker) =
        useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun RpcEndpointMarker.to(other: String) = useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun RpcEndpointMarker.to(other: MessageRef) =
        useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    override fun RpcEndpointMarker.to(other: RpcEndpointMarker) =
        useShorthand(endpoints.endpoint(this), endpoints.endpoint(other))

    fun build(): Rpc = Rpc(
        name = name,
        request =
        requireNotNull(request) {
            "RPC '$name' must define a request type"
        },
        response =
        requireNotNull(response) {
            "RPC '$name' must define a response type"
        },
    )

    private fun setExplicitRequest(endpoint: RpcEndpoint) {
        useDeclarationStyle(RpcDeclarationStyle.EXPLICIT)
        setRequest(endpoint)
    }

    private fun setExplicitResponse(endpoint: RpcEndpoint) {
        useDeclarationStyle(RpcDeclarationStyle.EXPLICIT)
        setResponse(endpoint)
    }

    private fun useShorthand(request: RpcEndpoint, response: RpcEndpoint) {
        useDeclarationStyle(RpcDeclarationStyle.SHORTHAND)
        setRequest(request)
        setResponse(response)
    }

    private fun setRequest(endpoint: RpcEndpoint) {
        require(request == null) {
            "RPC '$name' request already defined"
        }

        request = endpoint
    }

    private fun setResponse(endpoint: RpcEndpoint) {
        require(response == null) {
            "RPC '$name' response already defined"
        }

        response = endpoint
    }

    private fun useDeclarationStyle(style: RpcDeclarationStyle) {
        val currentStyle = declarationStyle

        if (currentStyle == null) {
            declarationStyle = style
            return
        }

        require(currentStyle == style) {
            "RPC '$name' cannot mix explicit request/response declarations with pair shorthand"
        }
    }

    override fun toString(): String = name

    private enum class RpcDeclarationStyle {
        EXPLICIT,
        SHORTHAND,
    }
}
