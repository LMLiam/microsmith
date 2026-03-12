package me.liam.microsmith.dsl.schemas.protobuf

import me.liam.microsmith.dsl.core.MicrosmithDsl
import me.liam.microsmith.dsl.schemas.protobuf.field.MapKeyType
import me.liam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import me.liam.microsmith.dsl.schemas.protobuf.field.Reference
import me.liam.microsmith.dsl.schemas.protobuf.field.ValueType

@Suppress("INAPPLICABLE_JVM_NAME")
@MicrosmithDsl
interface MapFieldScope : FieldScope {
    fun key(keyType: MapKeyType)

    fun value(valueType: ValueType)

    fun value(targetRef: String) = value(target = ref(targetRef))

    fun value(target: Reference) = value(valueType = target)

    fun types(blockValue: () -> Pair<MapKeyType, ValueType>) = types(kvpValue = blockValue())

    fun types(kvpValue: Pair<MapKeyType, ValueType>)

    fun types(keyType: MapKeyType, valueType: ValueType) = types(kvpValue = keyType to valueType)

    fun types(keyType: MapKeyType, target: String) = types(kvpRef = keyType to target)

    fun types(keyType: MapKeyType, target: Reference) = types(kvpValue = keyType to target)

    @JvmName("typesStr")
    fun types(kvpRef: Pair<MapKeyType, String>) = types(kvp = kvpRef.first to ref(kvpRef.second))

    @JvmName("typesRef")
    fun types(kvp: Pair<MapKeyType, Reference>) = types(kvpValue = kvp)

    @JvmName("typesPairRef")
    fun types(block: () -> Pair<MapKeyType, Reference>) = types(kvpValue = block())

    @JvmName("typesPairStr")
    fun types(blockRef: () -> Pair<MapKeyType, String>) = types(kvpRef = blockRef())

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
