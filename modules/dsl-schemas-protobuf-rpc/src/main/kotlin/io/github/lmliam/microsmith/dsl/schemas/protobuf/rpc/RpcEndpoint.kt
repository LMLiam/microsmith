package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference

data class RpcEndpoint(val reference: Reference, val streaming: Boolean = false)
