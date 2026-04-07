package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.math.BigDecimal
import java.math.BigInteger

internal fun requireRepresentableInteger(type: DotnetFieldType, value: Number): BigInteger {
    val integer = value.toExactBigDecimal().stripTrailingZeros().let { normalized ->
        require(normalized.scale() <= 0) {
            "ASP.NET request default '$value' is not representable as integer type '$type'."
        }
        normalized.toBigIntegerExact()
    }

    require(integer in integerRangeFor(type)) {
        "ASP.NET request default '$value' is out of range for integer type '$type'."
    }

    return integer
}

private fun Number.toExactBigDecimal(): BigDecimal = when (this) {
    is BigDecimal -> this
    is BigInteger -> BigDecimal(this)
    is Byte,
    is Short,
    is Int,
    is Long,
    -> BigDecimal.valueOf(this.toLong())

    is Float,
    is Double,
    -> BigDecimal.valueOf(this.toDouble())

    else -> BigDecimal(toString())
}

private fun integerRangeFor(type: DotnetFieldType): ClosedRange<BigInteger> = when (type) {
    DotnetFieldType.Byte -> BigInteger.ZERO..BYTE_MAX
    DotnetFieldType.SignedByte -> SBYTE_MIN..SBYTE_MAX
    DotnetFieldType.Short -> BigInteger.valueOf(Short.MIN_VALUE.toLong())..BigInteger.valueOf(Short.MAX_VALUE.toLong())
    DotnetFieldType.UnsignedShort -> BigInteger.ZERO..USHORT_MAX
    DotnetFieldType.Int -> BigInteger.valueOf(Int.MIN_VALUE.toLong())..BigInteger.valueOf(Int.MAX_VALUE.toLong())
    DotnetFieldType.UnsignedInt -> BigInteger.ZERO..UINT_MAX
    DotnetFieldType.Long,
    DotnetFieldType.NativeInt,
    -> BigInteger.valueOf(Long.MIN_VALUE)..BigInteger.valueOf(Long.MAX_VALUE)

    DotnetFieldType.UnsignedLong,
    DotnetFieldType.UnsignedNativeInt,
    -> BigInteger.ZERO..ULONG_MAX

    DotnetFieldType.Float,
    DotnetFieldType.Double,
    DotnetFieldType.Decimal,
    DotnetFieldType.String,
    DotnetFieldType.Char,
    DotnetFieldType.Bool,
    DotnetFieldType.Guid,
    DotnetFieldType.DateOnly,
    DotnetFieldType.TimeOnly,
    DotnetFieldType.DateTime,
    DotnetFieldType.DateTimeOffset,
    DotnetFieldType.TimeSpan,
    is DotnetFieldType.Reference,
    -> error("Unsupported ASP.NET integer literal type '$type'.")
}

private val BYTE_MAX = BigInteger.valueOf(255)
private val SBYTE_MIN = BigInteger.valueOf(-128)
private val SBYTE_MAX = BigInteger.valueOf(127)
private val USHORT_MAX = BigInteger.valueOf(65_535)
private val UINT_MAX = BigInteger("4294967295")
private val ULONG_MAX = BigInteger("18446744073709551615")
