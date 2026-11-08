package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

sealed interface CardinalityField : MessageField {
    val cardinality: Cardinality
}
