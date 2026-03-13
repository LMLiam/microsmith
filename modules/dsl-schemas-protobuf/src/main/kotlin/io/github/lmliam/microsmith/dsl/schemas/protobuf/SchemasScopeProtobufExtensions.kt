package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.schemas.core.SchemasBuilder
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.support.resolveReferences

fun SchemasScope.protobuf(block: ProtobufScope.() -> Unit) {
    val builder = ProtobufBuilder().apply(block)
    val schemasBuilder =
        this as? SchemasBuilder
            ?: error("protobuf { ... } can only be invoked within a SchemasBuilder scope.")
    resolveReferences(builder.build()).forEach { schemasBuilder.register(it) }
}
