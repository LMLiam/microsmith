package me.liam.microsmith.dsl.schemas.protobuf.field

sealed interface Field {
    val name: String
    val index: Int
}

sealed interface CardinalityField : Field {
    val cardinality: Cardinality
}