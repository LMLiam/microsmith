package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspDefaultValue

internal fun dotnetAspGuidLiteral(value: DotnetAspDefaultValue): String =
    "Guid.Parse(${dotnetAspStringLiteral(value.temporalTextValue())})"

internal fun dotnetAspDateOnlyLiteral(value: DotnetAspDefaultValue): String =
    "DateOnly.Parse(${dotnetAspStringLiteral(value.temporalTextValue())})"

internal fun dotnetAspTimeOnlyLiteral(value: DotnetAspDefaultValue): String =
    "TimeOnly.Parse(${dotnetAspStringLiteral(value.temporalTextValue())})"

internal fun dotnetAspDateTimeLiteral(value: DotnetAspDefaultValue): String =
    "DateTime.Parse(${dotnetAspStringLiteral(value.temporalTextValue())})"

internal fun dotnetAspDateTimeOffsetLiteral(value: DotnetAspDefaultValue): String =
    "DateTimeOffset.Parse(${dotnetAspStringLiteral(value.temporalTextValue())})"

internal fun dotnetAspTimeSpanLiteral(value: DotnetAspDefaultValue): String = when (value) {
    is DotnetAspDefaultValue.DurationValue ->
        "System.Xml.XmlConvert.ToTimeSpan(${dotnetAspStringLiteral(value.value.toString())})"

    is DotnetAspDefaultValue.StringValue,
    is DotnetAspDefaultValue.UuidValue,
    is DotnetAspDefaultValue.LocalDateValue,
    is DotnetAspDefaultValue.LocalTimeValue,
    is DotnetAspDefaultValue.LocalDateTimeValue,
    is DotnetAspDefaultValue.InstantValue,
    is DotnetAspDefaultValue.OffsetDateTimeValue,
    ->
        "TimeSpan.Parse(${dotnetAspStringLiteral(value.temporalTextValue())})"

    is DotnetAspDefaultValue.CharValue,
    is DotnetAspDefaultValue.NumericValue,
    is DotnetAspDefaultValue.BooleanValue,
    -> invalidTemporalDefaultValue(value)
}

private fun DotnetAspDefaultValue.temporalTextValue(): String = when (this) {
    is DotnetAspDefaultValue.StringValue -> value

    is DotnetAspDefaultValue.UuidValue -> value.toString()

    is DotnetAspDefaultValue.LocalDateValue -> value.toString()

    is DotnetAspDefaultValue.LocalTimeValue -> value.toString()

    is DotnetAspDefaultValue.LocalDateTimeValue -> value.toString()

    is DotnetAspDefaultValue.InstantValue -> value.toString()

    is DotnetAspDefaultValue.OffsetDateTimeValue -> value.toString()

    is DotnetAspDefaultValue.DurationValue -> value.toString()

    is DotnetAspDefaultValue.CharValue,
    is DotnetAspDefaultValue.NumericValue,
    is DotnetAspDefaultValue.BooleanValue,
    -> invalidTemporalDefaultValue(this)
}

private fun invalidTemporalDefaultValue(value: DotnetAspDefaultValue): Nothing {
    error("Expected a temporal/string ASP.NET default value, but was ${value::class.simpleName}.")
}
