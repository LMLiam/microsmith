package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

public data class ScalarField(
    override val name: String,
    override val index: Int,
    public val primitive: PrimitiveType,
    override val cardinality: Cardinality = Cardinality.SINGULAR,
) : CardinalityField
