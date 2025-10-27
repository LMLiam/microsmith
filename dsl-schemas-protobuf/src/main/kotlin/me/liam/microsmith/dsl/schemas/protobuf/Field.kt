package me.liam.microsmith.dsl.schemas.protobuf

enum class Cardinality {
    REQUIRED, // Default in proto3
    OPTIONAL,
    REPEATED
}

data class Field(
    val name: String,
    val type: FieldType,
    val index: Int,
    val cardinality: Cardinality = Cardinality.REQUIRED
)
