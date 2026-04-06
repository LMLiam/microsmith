package io.github.lmliam.microsmith.compile.services.dotnet.asp

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

internal fun dotnetAspGuidLiteral(value: Any): String =
    "Guid.Parse(${dotnetAspStringLiteral(value.stringLiteralValue())})"

internal fun dotnetAspDateOnlyLiteral(value: Any): String =
    "DateOnly.Parse(${dotnetAspStringLiteral(value.stringLiteralValue())})"

internal fun dotnetAspTimeOnlyLiteral(value: Any): String =
    "TimeOnly.Parse(${dotnetAspStringLiteral(value.stringLiteralValue())})"

internal fun dotnetAspDateTimeLiteral(value: Any): String =
    "DateTime.Parse(${dotnetAspStringLiteral(value.stringLiteralValue())})"

internal fun dotnetAspDateTimeOffsetLiteral(value: Any): String =
    "DateTimeOffset.Parse(${dotnetAspStringLiteral(value.stringLiteralValue())})"

internal fun dotnetAspTimeSpanLiteral(value: Any): String = when (value) {
    is Duration ->
        "System.Xml.XmlConvert.ToTimeSpan(${dotnetAspStringLiteral(value.toString())})"

    else ->
        "TimeSpan.Parse(${dotnetAspStringLiteral(value.toString())})"
}

private fun Any.stringLiteralValue(): String = when (this) {
    is UUID,
    is LocalDate,
    is LocalTime,
    is LocalDateTime,
    is Instant,
    is OffsetDateTime,
    -> toString()

    else -> toString()
}
