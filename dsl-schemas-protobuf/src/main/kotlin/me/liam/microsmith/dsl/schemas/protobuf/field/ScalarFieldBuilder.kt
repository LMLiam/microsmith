package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.ScalarFieldScope

class ScalarFieldBuilder(
    var index: Int? = null, var cardinality: Cardinality = Cardinality.REQUIRED
) : ScalarFieldScope {

    override fun optional() {
        require(cardinality == Cardinality.REQUIRED) {
            "Cardinality already set to $cardinality"
        }
        cardinality = Cardinality.OPTIONAL
    }

    override fun repeated() {
        require(cardinality == Cardinality.REQUIRED) {
            "Cardinality already set to $cardinality"
        }
        cardinality = Cardinality.REPEATED
    }

    override fun index(index: Int) {
        this.index = index
    }
}