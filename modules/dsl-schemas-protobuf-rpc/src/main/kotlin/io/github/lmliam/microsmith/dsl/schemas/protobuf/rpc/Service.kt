package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.ProtobufReferenceResolvableType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.ProtobufReferenceResolver
import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

data class Service(override val name: String, val rpcs: List<Rpc>) :
    Type,
    ProtobufReferenceResolvableType {
    override fun resolveReferences(resolver: ProtobufReferenceResolver): Type = copy(
        rpcs = rpcs.map { rpc -> rpc.resolveReferences(resolver, name) },
    )

    private fun Rpc.resolveReferences(resolver: ProtobufReferenceResolver, serviceName: String): Rpc = copy(
        request = request.resolveReference(resolver, "service $serviceName rpc '$name' request"),
        response = response.resolveReference(resolver, "service $serviceName rpc '$name' response"),
    )

    private fun RpcEndpoint.resolveReference(
        resolver: ProtobufReferenceResolver,
        referenceContext: String,
    ): RpcEndpoint = copy(reference = resolver.resolveReference(reference, referenceContext))
}
