package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.schemas.core.SchemasBuilder
import me.liam.microsmith.dsl.schemas.core.SchemasScope
import me.liam.microsmith.dsl.schemas.protobuf.field.Field
import me.liam.microsmith.dsl.schemas.protobuf.field.MapField
import me.liam.microsmith.dsl.schemas.protobuf.field.MapKeyType
import me.liam.microsmith.dsl.schemas.protobuf.field.MapValueType
import me.liam.microsmith.dsl.schemas.protobuf.field.OneofField
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveFieldType
import me.liam.microsmith.dsl.schemas.protobuf.field.ScalarField

@MicrosmithDsl
interface ProtobufScope {
    fun message(name: String, block: MessageScope.() -> Unit = {})
    fun enum(name: String, block: EnumScope.() -> Unit = {})
}

@MicrosmithDsl
interface MessageScope : MessageFields<ScalarFieldScope> {
    fun optional(field: ScalarField)
    fun optional(block: MessageScope.() -> ScalarField)
    fun repeated(field: ScalarField)
    fun repeated(block: MessageScope.() -> ScalarField)
    fun oneof(name: String, block: OneofScope.() -> Unit)
    fun reserved(vararg indexes: Int)
    fun reserved(vararg names: String)
}

@MicrosmithDsl
interface EnumScope {
    fun value(name: String)
    operator fun String.unaryPlus() = value(this)
}

@MicrosmithDsl
interface OneofScope : ScalarFields<OneofFieldScope, OneofField>

@MicrosmithDsl
interface ScalarFieldScope : FieldScope {
    fun optional()
    fun repeated()
}

@MicrosmithDsl
interface OneofFieldScope : FieldScope

@MicrosmithDsl
interface MapFieldScope : FieldScope {
    fun key(keyType: MapKeyType)
    fun value(valueType: MapValueType)
    fun types(kvp: Pair<MapKeyType, MapValueType>)
    fun kv(keyType: MapKeyType, valueType: MapValueType) = types(keyType to valueType)
    fun types(block: () -> Pair<MapKeyType, MapValueType>) = types(block())
    operator fun Pair<MapKeyType, MapValueType>.unaryPlus() = types(this)

    val int32 get() = PrimitiveFieldType.INT32
    val int64 get() = PrimitiveFieldType.INT64
    val uint32 get() = PrimitiveFieldType.UINT32
    val uint64 get() = PrimitiveFieldType.UINT64
    val sint32 get() = PrimitiveFieldType.SINT32
    val sint64 get() = PrimitiveFieldType.SINT64
    val fixed32 get() = PrimitiveFieldType.FIXED32
    val fixed64 get() = PrimitiveFieldType.FIXED64
    val sfixed32 get() = PrimitiveFieldType.SFIXED32
    val sfixed64 get() = PrimitiveFieldType.SFIXED64
    val float get() = PrimitiveFieldType.FLOAT
    val double get() = PrimitiveFieldType.DOUBLE
    val bytes get() = PrimitiveFieldType.BYTES
    val bool get() = PrimitiveFieldType.BOOL
    val string get() = PrimitiveFieldType.STRING
}

interface FieldScope {
    fun index(index: Int)
}

interface ScalarFields<TFieldScope : FieldScope, TField : Field> {
    fun int32(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun int64(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun uint32(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun uint64(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun sint32(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun sint64(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun fixed32(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun fixed64(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun sfixed32(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun sfixed64(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun float(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun double(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun string(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun bytes(name: String, block: TFieldScope.() -> Unit = {}): TField
    fun bool(name: String, block: TFieldScope.() -> Unit = {}): TField
}

interface MessageFields<TFieldScope : FieldScope> : ScalarFields<TFieldScope, ScalarField> {
    fun map(
        name: String,
        block: MapFieldScope.() -> Unit
    ): MapField
}

fun SchemasScope.protobuf(block: ProtobufScope.() -> Unit) {
    val builder = ProtobufBuilder().apply(block)
    builder.build().forEach { (this as SchemasBuilder).register(it) }
}