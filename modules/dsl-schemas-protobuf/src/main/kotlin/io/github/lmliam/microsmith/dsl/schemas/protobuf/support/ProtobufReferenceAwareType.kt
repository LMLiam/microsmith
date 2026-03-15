package io.github.lmliam.microsmith.dsl.schemas.protobuf.support

import io.github.lmliam.microsmith.dsl.schemas.protobuf.types.Type

interface ProtobufReferenceAwareType : Type {
    fun resolveReferences(context: ProtobufReferenceResolutionScope): Type
}
