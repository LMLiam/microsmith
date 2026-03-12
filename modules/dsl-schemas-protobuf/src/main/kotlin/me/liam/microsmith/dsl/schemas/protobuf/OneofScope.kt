package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField

@MicrosmithDsl
interface OneofScope : ScalarFields<OneofFieldScope, OneofField> {
    fun ref(name: String, target: String, block: OneofReferenceFieldScope.() -> Unit = {}): OneofField
}
