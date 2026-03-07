package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.schemas.protobuf.field.CardinalityField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Max
import me.liam.microsmith.dsl.schemas.protobuf.reserved.MaxRange

@Suppress("INAPPLICABLE_JVM_NAME")
@MicrosmithDsl
interface MessageScope :
    ScalarFields<ScalarFieldScope, ScalarField>,
    Reservable {
    fun optional(field: CardinalityField)

    fun optional(block: MessageScope.() -> CardinalityField)

    fun repeated(field: CardinalityField)

    fun repeated(block: MessageScope.() -> CardinalityField)

    fun oneof(name: String, block: OneofScope.() -> Unit)

    fun map(name: String, block: MapFieldScope.() -> Unit): MapField

    fun ref(name: String, target: String, block: ReferenceFieldScope.() -> Unit = {}): ReferenceField

    val max get() = Max

    operator fun Int.rangeTo(max: Max) = MaxRange(this)
}
