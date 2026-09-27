package io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.enum

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.Reservable
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf

@MicrosmithDsl
interface EnumScope : Reservable {
    fun value(name: String, block: EnumValueScope.() -> Unit = {})

    operator fun String.unaryPlus() = value(this)
}
