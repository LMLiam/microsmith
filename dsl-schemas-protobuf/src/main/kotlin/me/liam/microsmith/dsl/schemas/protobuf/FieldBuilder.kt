package me.liam.microsmith.dsl.schemas.protobuf

class FieldBuilder(var index: Int, var optional: Boolean) : FieldScope {
    override fun optional() {
        optional = true
    }

    override fun index(index: Int) {
        this.index = index
    }
}