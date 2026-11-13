package io.github.lmliam.microsmith.artifact.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc.ResolvedProtobufRpcEndpoint
import tech.mappie.api.ObjectMappie

internal object ProtobufRpcEndpointMapper : ObjectMappie<ResolvedProtobufRpcEndpoint, ProtobufRpcEndpoint>() {
    override fun map(from: ResolvedProtobufRpcEndpoint): ProtobufRpcEndpoint = mapping {
        to::typeName fromProperty from::target transform { it.fullyQualifiedName }
    }
}
