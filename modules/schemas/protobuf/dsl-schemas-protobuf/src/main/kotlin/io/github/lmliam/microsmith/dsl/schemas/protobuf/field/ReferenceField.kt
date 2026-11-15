package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

public data class ReferenceField(
    override val name: String,
    override val index: Int,
    public val reference: Reference,
    override val cardinality: Cardinality = Cardinality.SINGULAR,
) : CardinalityField
