package io.github.lmliam.microsmith.compile.services.dotnet.asp

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

internal fun dotnetAspGuidLiteral(value: Any): String =
    "Guid.Parse(${dotnetAspStringLiteral(dotnetAspGuidValue(value))})"

internal fun dotnetAspDateOnlyLiteral(value: Any): String =
    "DateOnly.Parse(${dotnetAspStringLiteral(dotnetAspDateOnlyValue(value))})"

internal fun dotnetAspTimeOnlyLiteral(value: Any): String =
    "TimeOnly.Parse(${dotnetAspStringLiteral(dotnetAspTimeOnlyValue(value))})"

internal fun dotnetAspDateTimeLiteral(value: Any): String =
    "DateTime.Parse(${dotnetAspStringLiteral(dotnetAspDateTimeValue(value))})"

internal fun dotnetAspDateTimeOffsetLiteral(value: Any): String =
    "DateTimeOffset.Parse(${dotnetAspStringLiteral(dotnetAspDateTimeOffsetValue(value))})"

internal fun dotnetAspTimeSpanLiteral(value: Any): String = when (value) {
    is Duration ->
        "System.Xml.XmlConvert.ToTimeSpan(${dotnetAspStringLiteral(value.toString())})"

    else ->
        "TimeSpan.Parse(${dotnetAspStringLiteral(value.toString())})"
}

private fun dotnetAspGuidValue(value: Any): String = when (value) {
    is UUID -> value.toString()
    else -> value.toString()
}

private fun dotnetAspDateOnlyValue(value: Any): String = when (value) {
    is LocalDate -> value.toString()
    else -> value.toString()
}

private fun dotnetAspTimeOnlyValue(value: Any): String = when (value) {
    is LocalTime -> value.toString()
    else -> value.toString()
}

private fun dotnetAspDateTimeValue(value: Any): String = when (value) {
    is LocalDateTime -> value.toString()
    is Instant -> value.toString()
    else -> value.toString()
}

private fun dotnetAspDateTimeOffsetValue(value: Any): String = when (value) {
    is OffsetDateTime -> value.toString()
    is Instant -> value.toString()
    else -> value.toString()
}
