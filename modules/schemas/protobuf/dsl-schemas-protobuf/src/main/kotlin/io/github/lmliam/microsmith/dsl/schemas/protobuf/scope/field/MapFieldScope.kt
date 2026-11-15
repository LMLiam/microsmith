package io.github.lmliam.microsmith.dsl.schemas.protobuf.scope.field

import io.github.lmliam.microsmith.dsl.MicrosmithDsl
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.MapKeyType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.PrimitiveType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.Reference
import io.github.lmliam.microsmith.dsl.schemas.protobuf.field.ValueType
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.reference.ProtobufTypeRef

@Suppress("INAPPLICABLE_JVM_NAME")
@MicrosmithDsl
public interface MapFieldScope : FieldScope {
    public fun key(keyType: MapKeyType)

    public fun value(valueType: ValueType)

    public fun value(targetRef: String) {
        value(target = ref(targetRef))
    }

    public fun value(target: Reference) {
        value(valueType = target)
    }

    public fun value(target: ProtobufTypeRef) {
        value(Reference.Symbolic(target))
    }

    public fun types(blockValue: () -> Pair<MapKeyType, ValueType>) {
        types(kvpValue = blockValue())
    }

    public fun types(kvpValue: Pair<MapKeyType, ValueType>)

    public fun types(keyType: MapKeyType, valueType: ValueType) {
        types(kvpValue = keyType to valueType)
    }

    public fun types(keyType: MapKeyType, target: String) {
        types(kvpRef = keyType to target)
    }

    public fun types(keyType: MapKeyType, target: Reference) {
        types(kvpValue = keyType to target)
    }

    public fun types(keyType: MapKeyType, target: ProtobufTypeRef) {
        types(keyType, Reference.Symbolic(target))
    }

    @JvmName("typesStr")
    public fun types(kvpRef: Pair<MapKeyType, String>) {
        types(kvp = kvpRef.first to ref(kvpRef.second))
    }

    @JvmName("typesRef")
    public fun types(kvp: Pair<MapKeyType, Reference>) {
        types(kvpValue = kvp)
    }

    @JvmName("typesPairRef")
    public fun types(block: () -> Pair<MapKeyType, Reference>) {
        types(kvpValue = block())
    }

    @JvmName("typesPairStr")
    public fun types(blockRef: () -> Pair<MapKeyType, String>) {
        types(kvpRef = blockRef())
    }

    public fun ref(target: String): Reference

    public val int32: PrimitiveType.INT32
        get() = PrimitiveType.INT32

    public val int64: PrimitiveType.INT64
        get() = PrimitiveType.INT64

    public val uint32: PrimitiveType.UINT32
        get() = PrimitiveType.UINT32

    public val uint64: PrimitiveType.UINT64
        get() = PrimitiveType.UINT64

    public val sint32: PrimitiveType.SINT32
        get() = PrimitiveType.SINT32

    public val sint64: PrimitiveType.SINT64
        get() = PrimitiveType.SINT64

    public val fixed32: PrimitiveType.FIXED32
        get() = PrimitiveType.FIXED32

    public val fixed64: PrimitiveType.FIXED64
        get() = PrimitiveType.FIXED64

    public val sfixed32: PrimitiveType.SFIXED32
        get() = PrimitiveType.SFIXED32

    public val sfixed64: PrimitiveType.SFIXED64
        get() = PrimitiveType.SFIXED64

    public val float: PrimitiveType.FLOAT
        get() = PrimitiveType.FLOAT

    public val double: PrimitiveType.DOUBLE
        get() = PrimitiveType.DOUBLE

    public val bytes: PrimitiveType.BYTES
        get() = PrimitiveType.BYTES

    public val bool: PrimitiveType.BOOL
        get() = PrimitiveType.BOOL

    public val string: PrimitiveType.STRING
        get() = PrimitiveType.STRING
}
