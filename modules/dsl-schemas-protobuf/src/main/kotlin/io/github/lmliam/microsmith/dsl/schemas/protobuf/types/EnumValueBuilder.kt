package io.github.lmliam.microsmith.dsl.schemas.protobuf.types

import io.github.lmliam.microsmith.dsl.schemas.protobuf.EnumValueScope

internal class EnumValueBuilder(var index: Int? = null) : EnumValueScope {
    override fun index(index: Int) {
        this.index = index
    }
}
