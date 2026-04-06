package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.math.BigDecimal
import java.util.Locale

internal fun dotnetAspLiteral(type: DotnetFieldType, value: Any): String = when (type) {
    DotnetFieldType.String -> dotnetAspStringLiteral(value.toString())
    DotnetFieldType.Char -> dotnetAspCharLiteral(value)
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
    -> dotnetAspNumericLiteral(type, value.number())
    DotnetFieldType.Bool -> value.toString().lowercase(Locale.ROOT)
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
    if (type == DotnetFieldType.Float) {
        return "${dotnetAspFloatingLiteral(value)}f"
    }
    if (type == DotnetFieldType.Double) {
        return dotnetAspFloatingLiteral(value)
    }
    if (type == DotnetFieldType.Decimal) {
        return "${dotnetAspDecimalLiteral(value)}m"
    }
    return dotnetAspIntegerLiteral(type, value)
}

private fun dotnetAspIntegerLiteral(type: DotnetFieldType, value: Number): String {
    if (type == DotnetFieldType.Byte || type == DotnetFieldType.SignedByte) {
        return value.toByte().toString()
    }
    if (type == DotnetFieldType.Short) {
        return value.toShort().toString()
    }
    if (type == DotnetFieldType.UnsignedShort) {
        return value.toInt().toString()
    }
    if (type == DotnetFieldType.Int) {
        return value.toInt().toString()
    }
    if (type == DotnetFieldType.UnsignedInt) {
        return "${value.toLong()}u"
    }
    if (type == DotnetFieldType.Long) {
        return "${value.toLong()}L"
    }
    if (type == DotnetFieldType.UnsignedLong) {
        return "${value.toLong()}UL"
    }
    if (type == DotnetFieldType.NativeInt) {
        return value.toLong().toString()
    }
    if (type == DotnetFieldType.UnsignedNativeInt) {
        return "${value.toLong()}u"
    }
    error("Unsupported ASP.NET integer literal type '$type'.")
}

private fun dotnetAspFloatingLiteral(value: Number): String {
    val rendered = value.toString()
    return if (rendered.any { it == '.' || it == 'e' || it == 'E' }) {
        rendered
    } else {
        "$rendered.0"
    }
}

private fun dotnetAspDecimalLiteral(value: Any): String = when (value) {
    is BigDecimal -> value.toPlainString()
    is Number -> value.toString()
    else -> value.toString()
}

private fun Any.number(): Number {
    require(this is Number) {
        "ASP.NET request defaults for numeric fields must be numeric, but was '$this'."
    }
    return this
}
