package io.github.lmliam.microsmith.dsl.schemas.protobuf.numbers

public object ProtobufNumberLimits {
    public const val FIELD_MIN: Int = 1
    public const val FIELD_MAX: Int = 536_870_911
    public val FIELD_IMPLEMENTATION_RESERVED: IntRange = 19_000..19_999
    public const val ENUM_MIN: Int = Int.MIN_VALUE
    public const val ENUM_MAX: Int = Int.MAX_VALUE
}
