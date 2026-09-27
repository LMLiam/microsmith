package io.github.lmliam.microsmith.dsl.schemas.protobuf.field
internal fun CardinalityField.withCardinality(cardinality: Cardinality): CardinalityField {
    require(this.cardinality == Cardinality.SINGULAR) {
        "Field cardinality already set to ${this.cardinality}"
    }

    return when (this) {
        is ReferenceField -> copy(cardinality = cardinality)
        is ScalarField -> copy(cardinality = cardinality)
    }
}
