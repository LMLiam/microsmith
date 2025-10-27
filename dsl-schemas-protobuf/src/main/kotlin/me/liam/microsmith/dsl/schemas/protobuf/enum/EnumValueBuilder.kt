package me.liam.microsmith.dsl.schemas.protobuf.enum

import me.liam.microsmith.dsl.schemas.protobuf.EnumValueScope

class EnumValueBuilder(var index: Int? = null) : EnumValueScope {
    override fun index(index: Int) {
        this.index = index
    }
}