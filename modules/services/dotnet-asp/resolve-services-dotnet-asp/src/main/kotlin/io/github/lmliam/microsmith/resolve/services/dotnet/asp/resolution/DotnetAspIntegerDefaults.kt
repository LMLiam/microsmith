package io.github.lmliam.microsmith.resolve.services.dotnet.asp.resolution

import arrow.core.Either
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType
import java.math.BigDecimal
import java.math.BigInteger

internal enum class IntegerDefaultProblem {
    NOT_INTEGRAL,
    OUT_OF_RANGE,
}

internal data class DotnetIntegerType(val csharpName: String, val range: ClosedRange<BigInteger>)

internal fun DotnetFieldType.integerType(): DotnetIntegerType? = INTEGER_TYPES[this]

internal fun Number.toIntegerDefault(type: DotnetIntegerType): Either<IntegerDefaultProblem, BigInteger> {
    val normalized = toExactBigDecimal().stripTrailingZeros()
    if (normalized.scale() > 0) return Either.Left(IntegerDefaultProblem.NOT_INTEGRAL)

    val integer = normalized.toBigIntegerExact()
    return if (integer in type.range) Either.Right(integer) else Either.Left(IntegerDefaultProblem.OUT_OF_RANGE)
}

private fun Number.toExactBigDecimal(): BigDecimal =
    when (this) {
        is BigDecimal -> this

        is BigInteger -> BigDecimal(this)

        is Byte,
        is Short,
        is Int,
        is Long -> BigDecimal.valueOf(toLong())

        is Float,
        is Double -> BigDecimal.valueOf(toDouble())

        else -> BigDecimal(toString())
    }

private val UNSIGNED_LONG_MAX = BigInteger("18446744073709551615")

private fun signed(name: String, min: Long, max: Long) =
    DotnetIntegerType(name, BigInteger.valueOf(min)..BigInteger.valueOf(max))

private fun unsigned(name: String, max: BigInteger) = DotnetIntegerType(name, BigInteger.ZERO..max)

private val INTEGER_TYPES =
    mapOf(
        DotnetFieldType.Byte to unsigned("byte", BigInteger.valueOf(255)),
        DotnetFieldType.SignedByte to signed("sbyte", -128, 127),
        DotnetFieldType.Short to signed("short", Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()),
        DotnetFieldType.UnsignedShort to unsigned("ushort", BigInteger.valueOf(65_535)),
        DotnetFieldType.Int to signed("int", Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()),
        DotnetFieldType.UnsignedInt to unsigned("uint", BigInteger.valueOf(4_294_967_295L)),
        DotnetFieldType.Long to signed("long", Long.MIN_VALUE, Long.MAX_VALUE),
        DotnetFieldType.NativeInt to signed("long", Long.MIN_VALUE, Long.MAX_VALUE),
        DotnetFieldType.UnsignedLong to unsigned("ulong", UNSIGNED_LONG_MAX),
        DotnetFieldType.UnsignedNativeInt to unsigned("ulong", UNSIGNED_LONG_MAX),
    )
