package me.liam.microsmith.dsl.schemas.protobuf.field

import me.liam.microsmith.dsl.schemas.protobuf.OneofFieldScope

internal class OneofFieldBuilder(var index: Int? = null) : OneofFieldScope {
    override fun index(index: Int) {
        this.index = index
    }
}
