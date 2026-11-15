package io.github.lmliam.microsmith.resolve.schemas.protobuf.numbers

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.leftNel
import arrow.core.raise.context.bind
import arrow.core.raise.context.either
import arrow.core.raise.context.zipOrAccumulate
import arrow.core.toEitherNel
import io.github.lmliam.microsmith.dsl.schemas.protobuf.numbers.ProtobufNumberLimits

@ConsistentCopyVisibility
public data class ReservedSpan
private constructor(public val first: Int, public val last: Int, public val domain: Domain, private val form: Form) {
    public val notation: String
        get() =
            when (form) {
                Form.SINGLE -> "$first"
                Form.RANGE -> "$first to $last"
                Form.TO_MAX -> "$first to max"
            }

    public val numbers: IntRange
        get() = first..last

    public enum class Domain(public val min: Int, public val max: Int) {
        MESSAGE(ProtobufNumberLimits.FIELD_MIN, ProtobufNumberLimits.FIELD_MAX),
        ENUM(ProtobufNumberLimits.ENUM_MIN, ProtobufNumberLimits.ENUM_MAX),
    }

    public sealed interface Rejection {
        public data class Descending(public val first: Int, public val last: Int) : Rejection

        public data class OutOfDomain(public val number: Int) : Rejection
    }

    private enum class Form {
        SINGLE,
        RANGE,
        TO_MAX,
    }

    public companion object {
        public fun single(number: Int, domain: Domain): EitherNel<Rejection, ReservedSpan> =
            inDomain(number, domain).map { ReservedSpan(number, number, domain, Form.SINGLE) }.toEitherNel()

        public fun range(first: Int, last: Int, domain: Domain): EitherNel<Rejection, ReservedSpan> =
            if (first > last) {
                Rejection.Descending(first, last).leftNel()
            } else {
                either {
                    zipOrAccumulate({ inDomain(first, domain).bind() }, { inDomain(last, domain).bind() }) { _, _ ->
                        ReservedSpan(first, last, domain, Form.RANGE)
                    }
                }
            }

        public fun toMax(first: Int, domain: Domain): EitherNel<Rejection, ReservedSpan> =
            inDomain(first, domain).map { ReservedSpan(first, domain.max, domain, Form.TO_MAX) }.toEitherNel()

        private fun inDomain(number: Int, domain: Domain): Either<Rejection, Int> =
            if (number in domain.min..domain.max) {
                Either.Right(number)
            } else {
                Either.Left(Rejection.OutOfDomain(number))
            }
    }
}
