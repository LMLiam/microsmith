package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl

@MicrosmithDsl
interface ScalarFieldScope : FieldScope {
    fun optional()

    fun repeated()
}
