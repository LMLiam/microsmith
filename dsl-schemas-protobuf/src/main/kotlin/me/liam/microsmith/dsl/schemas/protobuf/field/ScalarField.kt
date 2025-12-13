package me.liam.microsmith.dsl.schemas.protobuf.field

data class ScalarField(
    override val name: String,
    override val index: Int,
    val primitive: PrimitiveType,
    override val cardinality: Cardinality = Cardinality.REQUIRED
) : CardinalityField