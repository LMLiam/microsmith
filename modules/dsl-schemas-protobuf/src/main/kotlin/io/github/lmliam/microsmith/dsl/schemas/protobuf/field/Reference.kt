package io.github.lmliam.microsmith.dsl.schemas.protobuf.field
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.ProtobufTypeRef

sealed interface Reference : ValueType {
    data class Local(val target: String) : Reference
    data class Relative(val expression: String) : Reference
    data class Qualified(val qualifiedName: String) : Reference
    data class Symbolic(val target: ProtobufTypeRef) : Reference
}
