package io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.enum

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.Reservable
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf

@MicrosmithDsl
public interface EnumScope : Reservable {
    public fun value(name: String, block: EnumValueScope.() -> Unit = {})

    public operator fun String.unaryPlus() {
        value(this)
    }
}
