package io.github.lmliam.microsmith.dsl.schemas.protobuf.support

import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference

interface ProtobufReferenceResolutionScope {
    fun resolveReference(reference: Reference, context: String): Reference
}
