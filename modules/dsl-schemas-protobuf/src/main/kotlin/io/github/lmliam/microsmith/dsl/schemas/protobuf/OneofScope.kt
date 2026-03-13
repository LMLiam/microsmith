package io.github.lmliam.microsmith.dsl.schemas.protobuf

import io.github.lmliam.microsmith.dsl.core.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.OneofField

@MicrosmithDsl
interface OneofScope : ScalarFields<OneofFieldScope, OneofField> {
    fun ref(name: String, target: String, block: OneofReferenceFieldScope.() -> Unit = {}): OneofField
}
