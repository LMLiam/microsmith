package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

@JvmInline
value class EnumValueNumber(val value: Int) {
    override fun toString(): String = value.toString()
}
