package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.ScalarFieldScope

internal class ScalarFieldBuilder(var index: Int? = null, var cardinality: Cardinality = Cardinality.SINGULAR) :
    ScalarFieldScope {
    override fun optional() {
        require(cardinality == Cardinality.SINGULAR) { "Cardinality already set to $cardinality" }
        cardinality = Cardinality.OPTIONAL
    }

    override fun repeated() {
        require(cardinality == Cardinality.SINGULAR) { "Cardinality already set to $cardinality" }
        cardinality = Cardinality.REPEATED
    }

    override fun index(index: Int) {
        this.index = index
    }
}
