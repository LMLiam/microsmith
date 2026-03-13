package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

sealed interface CardinalityField : Field {
    val cardinality: Cardinality
}
