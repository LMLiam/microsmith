package io.github.lmliam.microsmith.dsl.schemas.protobuf.support

import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufSchema

internal fun resolveReferences(schemas: Set<ProtobufSchema>): Set<ProtobufSchema> {
    val resolver = ReferenceResolutionContext(schemas)
    val resolvedSchemas = schemas.map(resolver::resolve).toSet()
    resolver.failOnUnresolvedReferences()
    return resolvedSchemas
}
