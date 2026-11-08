package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

import arrow.core.Either

@JvmInline
value class FieldNumber private constructor(val value: Int) {
    override fun toString(): String = value.toString()

    companion object {
        const val MIN_VALUE = 1
        const val MAX_VALUE = 536_870_911
        val IMPLEMENTATION_RESERVED_RANGE = 19_000..19_999

        fun parse(raw: Int): Either<Int, FieldNumber> =
            if (raw in MIN_VALUE..MAX_VALUE && raw !in IMPLEMENTATION_RESERVED_RANGE) {
                Either.Right(FieldNumber(raw))
            } else {
                Either.Left(raw)
            }
    }
}
