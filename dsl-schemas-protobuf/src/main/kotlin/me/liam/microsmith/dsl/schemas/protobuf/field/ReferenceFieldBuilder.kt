package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.ReferenceFieldScope

class ReferenceFieldBuilder(
    var index: Int? = null
) : ReferenceFieldScope {
    override fun index(index: Int) {
        this.index = index
    }
}