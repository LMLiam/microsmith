package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

public sealed interface CardinalityField : MessageField {
    public val cardinality: Cardinality
}
