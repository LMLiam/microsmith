package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.ProtobufReferenceResolutionScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.ProtobufReferenceResolvableType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

data class Service(
    override val name: String,
    val rpcs: List<Rpc>,
) : Type, ProtobufReferenceResolvableType {
    override fun resolveReferences(context: ProtobufReferenceResolutionScope): Type = copy(
        rpcs = rpcs.map { rpc -> rpc.resolveReferences(context, name) },
    )

    private fun Rpc.resolveReferences(context: ProtobufReferenceResolutionScope, serviceName: String): Rpc = copy(
        request = request.resolveReference(context, "service $serviceName rpc '$name' request"),
        response = response.resolveReference(context, "service $serviceName rpc '$name' response"),
    )

    private fun RpcEndpoint.resolveReference(
        context: ProtobufReferenceResolutionScope,
        referenceContext: String,
    ): RpcEndpoint = copy(reference = context.resolveReference(reference, referenceContext))
}
