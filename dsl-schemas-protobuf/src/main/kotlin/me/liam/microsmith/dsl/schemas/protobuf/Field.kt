package me.liam.microsmith.dsl.schemas.protobuf

enum class Cardinality {
    REQUIRED, // Default in proto3
    OPTIONAL,
    REPEATED
}

sealed interface Field {
    val name: String
    val index: Int
}

data class ScalarField(
    override val name: String,
    override val index: Int,
    val primitive: PrimitiveFieldType,
    val cardinality: Cardinality = Cardinality.REQUIRED
) : Field

data class MapField(
    override val name: String,
    override val index: Int,
    val type: MapFieldType
) : Field

data class OneofField(
    override val name: String,
    override val index: Int,
    val primitive: PrimitiveFieldType
) : Field