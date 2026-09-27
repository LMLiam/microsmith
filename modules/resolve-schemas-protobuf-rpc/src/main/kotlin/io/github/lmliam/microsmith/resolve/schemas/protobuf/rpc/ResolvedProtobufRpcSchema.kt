package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

data class ResolvedProtobufRpcSchema(
    val qualifiedName: QualifiedSchemaName,
    val imports: List<String>,
    val rpcs: List<ResolvedProtobufRpc>,
)
