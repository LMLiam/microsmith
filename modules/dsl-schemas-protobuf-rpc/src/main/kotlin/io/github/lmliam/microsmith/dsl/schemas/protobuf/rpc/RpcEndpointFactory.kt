package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufDeclarationContext
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.MessageRef

internal class RpcEndpointFactory(private val declarationContext: ProtobufDeclarationContext) {
    fun build(target: String, block: RpcEndpointScope.() -> Unit): RpcEndpoint = buildResolved(
        qualifiedName = resolve(target),
        block = block,
    )

    fun build(target: MessageRef, block: RpcEndpointScope.() -> Unit): RpcEndpoint = buildResolved(
        qualifiedName = target.qualifiedName,
        block = block,
    )

    fun stream(target: String): RpcEndpointMarker = RpcEndpointMarker(
        target = resolve(target),
        streaming = true,
    )

    fun stream(target: MessageRef): RpcEndpointMarker = RpcEndpointMarker(
        target = target.qualifiedName,
        streaming = true,
    )

    fun endpoint(target: String): RpcEndpoint = createEndpoint(resolve(target))

    fun endpoint(target: MessageRef): RpcEndpoint = createEndpoint(target.qualifiedName)

    fun endpoint(marker: RpcEndpointMarker): RpcEndpoint = createEndpoint(
        qualifiedName = marker.target,
        streaming = marker.streaming,
    )

    private fun buildResolved(qualifiedName: String, block: RpcEndpointScope.() -> Unit): RpcEndpoint {
        val scope = RpcEndpointScopeBuilder().apply(block)

        return createEndpoint(
            qualifiedName = qualifiedName,
            streaming = scope.streaming,
        )
    }

    private fun resolve(target: String): String = declarationContext.resolveReference(target)

    private fun createEndpoint(qualifiedName: String, streaming: Boolean = false): RpcEndpoint = RpcEndpoint(
        reference = Reference.Qualified(qualifiedName),
        streaming = streaming,
    )

    private class RpcEndpointScopeBuilder : RpcEndpointScope {
        var streaming: Boolean = false
            private set

        override fun stream() {
            require(!streaming) {
                "stream() already set for RPC endpoint"
            }

            streaming = true
        }
    }
}
