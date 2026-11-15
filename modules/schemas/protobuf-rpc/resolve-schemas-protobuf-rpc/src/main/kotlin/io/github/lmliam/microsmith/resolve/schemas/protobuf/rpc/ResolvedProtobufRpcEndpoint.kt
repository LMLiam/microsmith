package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.schemas.protobuf.names.QualifiedSchemaName

public data class ResolvedProtobufRpcEndpoint(public val target: QualifiedSchemaName, public val streaming: Boolean)
