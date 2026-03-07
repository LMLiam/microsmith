package me.liam.microsmith.dsl.schemas.protobuf.field

data class ReferenceField(
    override val name: String,
    override val index: Int,
    val reference: Reference,
    override val cardinality: Cardinality = Cardinality.REQUIRED,
) : CardinalityField
