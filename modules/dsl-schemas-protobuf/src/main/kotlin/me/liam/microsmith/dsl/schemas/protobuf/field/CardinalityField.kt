package me.liam.microsmith.dsl.schemas.protobuf.field

sealed interface CardinalityField : Field {
    val cardinality: Cardinality
}
