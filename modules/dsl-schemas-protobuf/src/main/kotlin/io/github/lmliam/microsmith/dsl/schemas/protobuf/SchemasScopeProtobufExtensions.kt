package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.SchemasBuilder
import io.github.lmliam.microsmith.dsl.schemas.SchemasScope

fun SchemasScope.protobuf(block: ProtobufScope.() -> Unit) {
    val builder = ProtobufBuilder().apply(block)
    val schemasBuilder = this as? SchemasBuilder
        ?: error("protobuf { ... } can only be invoked within a SchemasBuilder scope.")

    builder.build()
        .forEach(schemasBuilder::register)
}
