package me.liam.microsmith.dsl.schemas.protobuf

class FieldBuilder(var index: Int, var cardinality: Cardinality = Cardinality.REQUIRED) : FieldScope {
    override fun optional() {
        require(cardinality == Cardinality.REQUIRED) { "Cardinality already set to $cardinality" }
        cardinality = Cardinality.OPTIONAL
    }

    override fun repeated() {
        require(cardinality == Cardinality.REQUIRED) { "Cardinality already set to $cardinality" }
        cardinality = Cardinality.REPEATED
    }

    override fun index(index: Int) {
        this.index = index
    }
}