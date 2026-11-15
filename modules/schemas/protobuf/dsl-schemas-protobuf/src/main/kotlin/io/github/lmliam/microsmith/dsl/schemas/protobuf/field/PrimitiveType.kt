package io.github.lmliam.microsmith.dsl.schemas.protobuf.field

public sealed interface PrimitiveType : ValueType {
    public object INT32 : MapKeyType

    public object INT64 : MapKeyType

    public object UINT32 : MapKeyType

    public object UINT64 : MapKeyType

    public object SINT32 : MapKeyType

    public object SINT64 : MapKeyType

    public object FIXED32 : MapKeyType

    public object FIXED64 : MapKeyType

    public object SFIXED32 : MapKeyType

    public object SFIXED64 : MapKeyType

    public object STRING : MapKeyType

    public object BOOL : MapKeyType

    public object FLOAT : PrimitiveType

    public object DOUBLE : PrimitiveType

    public object BYTES : PrimitiveType
}
