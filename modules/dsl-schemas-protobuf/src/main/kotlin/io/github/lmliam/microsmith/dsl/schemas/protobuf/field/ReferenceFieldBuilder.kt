package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.ReferenceFieldScope

internal class ReferenceFieldBuilder(var index: Int? = null, var cardinality: Cardinality = Cardinality.SINGULAR) :
    ReferenceFieldScope {
    override fun index(index: Int) {
        this.index = index
    }

    override fun optional() {
        require(cardinality == Cardinality.SINGULAR) {
            "Cardinality is already set to $cardinality"
        }
        this.cardinality = Cardinality.OPTIONAL
    }

    override fun repeated() {
        require(cardinality == Cardinality.SINGULAR) {
            "Cardinality is already set to $cardinality"
        }
        this.cardinality = Cardinality.REPEATED
    }
}
