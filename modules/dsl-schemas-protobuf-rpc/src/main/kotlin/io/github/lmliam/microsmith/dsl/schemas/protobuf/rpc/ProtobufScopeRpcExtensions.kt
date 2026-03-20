package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufDeclarationContext
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufScope

fun ProtobufScope.service(name: String, block: ServiceScope.() -> Unit = {}) {
    val declarationScope =
        this as? ProtobufDeclarationContext
            ?: error("service { ... } can only be invoked within a protobuf declaration context.")

    declarationScope.registerDeclaration(
        name,
        ServiceBuilder(name, declarationScope::resolveReference).apply(block).build(),
    )
}
