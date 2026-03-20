package io.github.lmliam.microsmith.dsl.schemas.protobuf.support

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference

interface ProtobufReferenceResolver {
    fun resolveReference(reference: Reference, context: String): Reference
}
