package me.liam.microsmith.dsl.schemas.protobuf

class EnumValueBuilder(var index: Int? = null) : EnumValueScope {
    override fun index(index: Int) {
        this.index = index
    }
}