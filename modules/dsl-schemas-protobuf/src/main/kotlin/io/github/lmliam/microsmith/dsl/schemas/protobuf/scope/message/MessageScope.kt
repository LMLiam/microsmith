package io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.message
import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.Reservable
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ScalarFields
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.CardinalityField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ReferenceField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ScalarField
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.ProtobufTypeRef
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.Max
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.MapFieldScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.ReferenceFieldScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field.ScalarFieldScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.oneof.OneofScope

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

    fun ref(name: String, target: ProtobufTypeRef, block: ReferenceFieldScope.() -> Unit = {}): ReferenceField

    val max get() = Max

    operator fun Int.rangeTo(max: Max) = MaxRange(this)
}
