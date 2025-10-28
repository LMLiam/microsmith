package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.schemas.core.SchemasBuilder
import me.liam.microsmith.dsl.schemas.core.SchemasScope
import me.liam.microsmith.dsl.schemas.protobuf.field.*
import me.liam.microsmith.dsl.schemas.protobuf.reserved.Max
import me.liam.microsmith.dsl.schemas.protobuf.reserved.MaxRange
import me.liam.microsmith.dsl.schemas.protobuf.support.resolveReferences

@MicrosmithDsl
interface ProtobufScope {
    operator fun String.invoke(block: ProtobufScope.() -> Unit)
    operator fun Int.invoke(block: ProtobufScope.() -> Unit) = version(this, block)
    fun version(version: Int, block: ProtobufScope.() -> Unit)

    fun message(name: String, block: MessageScope.() -> Unit = {})
    fun enum(name: String, block: EnumScope.() -> Unit = {})
}

interface Reservable {
    fun reserved(vararg indexes: Int)
    fun reserved(vararg indexRanges: IntRange) = indexRanges.forEach { reserved(*it.toSet().toIntArray()) }
    fun reserved(vararg names: String)
    fun reserved(toMax: MaxRange)
    fun reserved(block: ReservedScope.() -> Unit)
}

@MicrosmithDsl
interface MessageScope : ScalarFields<ScalarFieldScope, ScalarField>, Reservable {
    fun optional(field: ScalarField)
    fun optional(block: MessageScope.() -> ScalarField)
    fun repeated(field: ScalarField)
    fun repeated(block: MessageScope.() -> ScalarField)
    fun oneof(name: String, block: OneofScope.() -> Unit)
    fun map(name: String, block: MapFieldScope.() -> Unit): MapField
    fun ref(name: String, target: String, block: ReferenceFieldScope.() -> Unit = {}): ReferenceField
}

@MicrosmithDsl
interface ReservedScope {
    val max get() = Max

    fun index(index: Int)
    fun name(name: String)
    fun range(range: IntRange)
    fun range(range: MaxRange)
    fun range(start: Int, end: Int) = range(start..end)

    operator fun String.unaryPlus() = name(this)
    operator fun IntRange.unaryPlus() = range(this)
    operator fun MaxRange.unaryPlus() = range(this)
    operator fun Int.rangeTo(max: Max) = MaxRange(this)
}

@MicrosmithDsl
interface EnumScope : Reservable {
    fun value(name: String, block: EnumValueScope.() -> Unit = {})
    operator fun String.unaryPlus() = value(this)
}

@MicrosmithDsl
interface EnumValueScope : FieldScope

@MicrosmithDsl
interface OneofScope : ScalarFields<OneofFieldScope, OneofField> {
    fun ref(name: String, target: String, block: ReferenceFieldScope.() -> Unit = {}): OneofField
}

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
    fun value(valueType: ValueType)
    fun value(targetRef: String) = value(target = ref(targetRef))
    fun value(target: Reference) = value(valueType = target)
    fun types(blockValue: () -> Pair<MapKeyType, ValueType>) = types(blockValue())
    fun types(kvpValue: Pair<MapKeyType, ValueType>)
    fun types(keyType: MapKeyType, valueType: ValueType) = types(keyType to valueType)
    fun types(keyType: MapKeyType, target: String) = types(keyType to target)
    fun types(keyType: MapKeyType, target: Reference) = types(keyType to target)

    fun ref(target: String): Reference

    val int32 get() = PrimitiveType.INT32
    val int64 get() = PrimitiveType.INT64
    val uint32 get() = PrimitiveType.UINT32
    val uint64 get() = PrimitiveType.UINT64
    val sint32 get() = PrimitiveType.SINT32
    val sint64 get() = PrimitiveType.SINT64
    val fixed32 get() = PrimitiveType.FIXED32
    val fixed64 get() = PrimitiveType.FIXED64
    val sfixed32 get() = PrimitiveType.SFIXED32
    val sfixed64 get() = PrimitiveType.SFIXED64
    val float get() = PrimitiveType.FLOAT
    val double get() = PrimitiveType.DOUBLE
    val bytes get() = PrimitiveType.BYTES
    val bool get() = PrimitiveType.BOOL
    val string get() = PrimitiveType.STRING
}

@JvmName("typesStr")
fun MapFieldScope.types(kvpRef: Pair<MapKeyType, String>) = types(kvp = kvpRef.first to ref(kvpRef.second))
@JvmName("typesRef")
fun MapFieldScope.types(kvp: Pair<MapKeyType, Reference>) = types(kvpValue = kvp)
@JvmName("typesPairRef")
fun MapFieldScope.types(block: () -> Pair<MapKeyType, Reference>) = types(block())
@JvmName("typesPairStr")
fun MapFieldScope.types(blockRef: () -> Pair<MapKeyType, String>) = types(kvpRef = blockRef())

@MicrosmithDsl
interface ReferenceFieldScope : FieldScope

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

fun SchemasScope.protobuf(block: ProtobufScope.() -> Unit) {
    val builder = ProtobufBuilder().apply(block)
    builder.build()
        .also { resolveReferences(it) }
        .forEach { (this as SchemasBuilder).register(it) }
}