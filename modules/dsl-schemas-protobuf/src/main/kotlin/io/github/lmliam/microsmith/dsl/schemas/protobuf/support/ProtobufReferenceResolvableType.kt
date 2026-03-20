package io.github.lmliam.microsmith.dsl.schemas.protobuf.support

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

interface ProtobufReferenceResolvableType : Type {
    fun resolveReferences(resolver: ProtobufReferenceResolver): Type
}
