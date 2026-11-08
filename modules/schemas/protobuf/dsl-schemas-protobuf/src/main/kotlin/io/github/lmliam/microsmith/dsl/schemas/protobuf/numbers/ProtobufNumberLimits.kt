package io.github.lmliam.microsmith.dsl.schemas.protobuf.numbers

object ProtobufNumberLimits {
    const val FIELD_MIN = 1
    const val FIELD_MAX = 536_870_911
    val FIELD_IMPLEMENTATION_RESERVED = 19_000..19_999
    const val ENUM_MIN = Int.MIN_VALUE
    const val ENUM_MAX = Int.MAX_VALUE
}
