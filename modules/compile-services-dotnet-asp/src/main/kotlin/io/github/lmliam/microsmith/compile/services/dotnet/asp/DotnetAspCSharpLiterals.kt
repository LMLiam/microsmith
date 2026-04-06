package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspDefaultValue
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.math.BigDecimal
import java.util.Locale

internal fun dotnetAspLiteral(type: DotnetFieldType, value: DotnetAspDefaultValue): String = when (type) {
    DotnetFieldType.String -> dotnetAspStringLiteral(value.requireString())
    DotnetFieldType.Char -> dotnetAspCharLiteral(value.requireChar())
    DotnetFieldType.Byte,
    DotnetFieldType.SignedByte,
    DotnetFieldType.Short,
    DotnetFieldType.UnsignedShort,
    DotnetFieldType.Int,
    DotnetFieldType.UnsignedInt,
    DotnetFieldType.Long,
    DotnetFieldType.UnsignedLong,
    DotnetFieldType.NativeInt,
    DotnetFieldType.UnsignedNativeInt,
    DotnetFieldType.Float,
    DotnetFieldType.Double,
    DotnetFieldType.Decimal,
    -> dotnetAspNumericLiteral(type, value.requireNumber())
    DotnetFieldType.Bool -> value.requireBoolean().toString().lowercase(Locale.ROOT)
    DotnetFieldType.Guid -> dotnetAspGuidLiteral(value)
    DotnetFieldType.DateOnly -> dotnetAspDateOnlyLiteral(value)
    DotnetFieldType.TimeOnly -> dotnetAspTimeOnlyLiteral(value)
    DotnetFieldType.DateTime -> dotnetAspDateTimeLiteral(value)
    DotnetFieldType.DateTimeOffset -> dotnetAspDateTimeOffsetLiteral(value)
    DotnetFieldType.TimeSpan -> dotnetAspTimeSpanLiteral(value)
    is DotnetFieldType.Reference ->
        error("ASP.NET request defaults cannot target shared model '${type.target}'.")
}
private fun dotnetAspNumericLiteral(type: DotnetFieldType, value: Number): String {
    return when (type) {
        DotnetFieldType.Float -> "${dotnetAspFloatingLiteral(value)}f"
        DotnetFieldType.Double -> dotnetAspFloatingLiteral(value)
        DotnetFieldType.Decimal -> "${dotnetAspDecimalLiteral(value)}m"
        DotnetFieldType.Byte,
        DotnetFieldType.SignedByte,
        DotnetFieldType.Short,
        DotnetFieldType.UnsignedShort,
        DotnetFieldType.Int,
        DotnetFieldType.UnsignedInt,
        DotnetFieldType.Long,
        DotnetFieldType.UnsignedLong,
        DotnetFieldType.NativeInt,
        DotnetFieldType.UnsignedNativeInt,
        -> dotnetAspIntegerLiteral(type, value)

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
        -> error("Unsupported ASP.NET numeric literal type '$type'.")
    }
}

private fun dotnetAspIntegerLiteral(type: DotnetFieldType, value: Number): String {
    return when (type) {
        DotnetFieldType.Byte,
        DotnetFieldType.SignedByte,
        -> value.toByte().toString()

        DotnetFieldType.Short -> value.toShort().toString()

        DotnetFieldType.UnsignedShort,
        DotnetFieldType.Int,
        -> value.toInt().toString()

        DotnetFieldType.UnsignedInt,
        DotnetFieldType.UnsignedNativeInt,
        -> "${value.toLong()}u"

        DotnetFieldType.Long -> "${value.toLong()}L"
        DotnetFieldType.UnsignedLong -> "${value.toLong()}UL"
        DotnetFieldType.NativeInt -> value.toLong().toString()

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
}

private fun dotnetAspFloatingLiteral(value: Number): String {
    val rendered = value.toString()
    return if (rendered.any { it == '.' || it == 'e' || it == 'E' }) {
        rendered
    } else {
        "$rendered.0"
    }
}

private fun dotnetAspDecimalLiteral(value: Number): String = if (value is BigDecimal) {
    value.toPlainString()
} else {
    value.toString()
}
