package me.liam.microsmith.dsl

@MicrosmithDsl
interface MicrosmithScope

fun microsmith(block: MicrosmithScope.() -> Unit): MicrosmithModel {
    return MicrosmithBuilder().apply(block).build()
}