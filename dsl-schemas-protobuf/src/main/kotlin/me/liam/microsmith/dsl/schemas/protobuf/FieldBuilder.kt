package me.liam.microsmith.dsl.schemas.protobuf

class MessageFieldBuilder(
    var index: Int,
    var cardinality: Cardinality = Cardinality.REQUIRED
) : MessageFieldScope {
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

class OneofFieldBuilder(
    var index: Int
) : OneofFieldScope {
    override fun index(index: Int) {
        this.index = index
    }
}