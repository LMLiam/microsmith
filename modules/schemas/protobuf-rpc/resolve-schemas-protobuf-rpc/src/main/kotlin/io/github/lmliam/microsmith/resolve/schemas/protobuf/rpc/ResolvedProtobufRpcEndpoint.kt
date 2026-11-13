package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

data class ResolvedProtobufRpcEndpoint(val target: QualifiedSchemaName, val streaming: Boolean)
