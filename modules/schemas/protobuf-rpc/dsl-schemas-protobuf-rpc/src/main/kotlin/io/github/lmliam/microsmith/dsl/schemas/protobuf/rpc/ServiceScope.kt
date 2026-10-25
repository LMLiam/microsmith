package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.MicrosmithDsl

@MicrosmithDsl
interface ServiceScope {
    operator fun String.invoke(block: RpcScope.() -> Unit = {})
}
