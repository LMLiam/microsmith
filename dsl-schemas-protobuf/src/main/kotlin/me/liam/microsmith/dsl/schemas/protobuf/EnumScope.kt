package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface EnumScope : Reservable {
    fun value(name: String, block: EnumValueScope.() -> Unit = {})

    operator fun String.unaryPlus() = value(this)
}
