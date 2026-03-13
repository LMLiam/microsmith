package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

import io.github.lmliam.microsmith.dsl.schemas.protobuf.OneofFieldScope

internal class OneofFieldBuilder(var index: Int? = null) : OneofFieldScope {
    override fun index(index: Int) {
        this.index = index
    }
}
