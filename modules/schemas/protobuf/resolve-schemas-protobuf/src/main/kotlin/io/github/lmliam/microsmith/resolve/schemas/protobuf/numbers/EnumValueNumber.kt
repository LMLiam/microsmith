package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

@JvmInline
public value class EnumValueNumber(public val value: Int) {
    override fun toString(): String = value.toString()
}
