package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

import arrow.core.Either
import io.github.lmliam.microsmith.dsl.schemas.protobuf.numbers.ProtobufNumberLimits

@JvmInline
public value class FieldNumber private constructor(public val value: Int) {
    override fun toString(): String = value.toString()

    public companion object {
        private val RANGE = ProtobufNumberLimits.FIELD_MIN..ProtobufNumberLimits.FIELD_MAX

        public fun parse(raw: Int): Either<Int, FieldNumber> =
            if (raw in RANGE && raw !in ProtobufNumberLimits.FIELD_IMPLEMENTATION_RESERVED) {
                Either.Right(FieldNumber(raw))
            } else {
                Either.Left(raw)
            }
    }
}
