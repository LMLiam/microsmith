package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.leftNel
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.zipOrAccumulate
import arrow.core.toEitherNel

@ConsistentCopyVisibility
data class ReservedSpan private constructor(val first: Int, val last: Int, val domain: Domain, private val form: Form) {
    val notation: String
        get() =
            when (form) {
                Form.SINGLE -> "$first"
                Form.RANGE -> "$first to $last"
                Form.TO_MAX -> "$first to max"
            }

    val numbers: IntRange
        get() = first..last

    operator fun contains(number: Int): Boolean = number in numbers

    enum class Domain(val min: Int, val max: Int) {
        MESSAGE(FieldNumber.MIN_VALUE, FieldNumber.MAX_VALUE),
        ENUM(Int.MIN_VALUE, Int.MAX_VALUE),
    }

    sealed interface Rejection {
        data class Descending(val first: Int, val last: Int) : Rejection

        data class OutOfDomain(val number: Int) : Rejection
    }

    private enum class Form {
        SINGLE,
        RANGE,
        TO_MAX,
    }

    companion object {
        fun single(number: Int, domain: Domain): EitherNel<Rejection, ReservedSpan> =
            inDomain(number, domain).map { ReservedSpan(number, number, domain, Form.SINGLE) }.toEitherNel()

        fun range(first: Int, last: Int, domain: Domain): EitherNel<Rejection, ReservedSpan> =
            if (first > last) {
                Rejection.Descending(first, last).leftNel()
            } else {
                either {
                    zipOrAccumulate({ inDomain(first, domain).bind() }, { inDomain(last, domain).bind() }) { _, _ ->
                        ReservedSpan(first, last, domain, Form.RANGE)
                    }
                }
            }

        fun toMax(first: Int, domain: Domain): EitherNel<Rejection, ReservedSpan> =
            inDomain(first, domain).map { ReservedSpan(first, domain.max, domain, Form.TO_MAX) }.toEitherNel()

        private fun inDomain(number: Int, domain: Domain): Either<Rejection, Int> =
            if (number in domain.min..domain.max) {
                Either.Right(number)
            } else {
                Either.Left(Rejection.OutOfDomain(number))
            }
    }
}
