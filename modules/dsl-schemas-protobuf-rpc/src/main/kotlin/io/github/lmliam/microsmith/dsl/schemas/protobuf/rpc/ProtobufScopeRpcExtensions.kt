package io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufDeclarationScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufScope

fun ProtobufScope.service(name: String, block: ServiceScope.() -> Unit = {}) {
    val declarationScope =
        this as? ProtobufDeclarationScope
            ?: error("service { ... } can only be invoked within a protobuf declaration scope.")

    declarationScope.registerDeclaration(
        name,
        ServiceBuilder(name, declarationScope::resolveReference).apply(block).build(),
    )
}
