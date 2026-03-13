package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface EnumScope : Reservable {
    fun value(name: String, block: EnumValueScope.() -> Unit = {})

    operator fun String.unaryPlus() = value(this)
}
