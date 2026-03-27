package io.github.lmliam.microsmith.resolve.schemas.protobuf.rpc

import io.github.lmliam.microsmith.resolve.core.ResolvedModel

data class ResolvedProtobufRpcSchemaModel(
    val schemas: List<ResolvedProtobufRpcSchema>,
) : ResolvedModel
