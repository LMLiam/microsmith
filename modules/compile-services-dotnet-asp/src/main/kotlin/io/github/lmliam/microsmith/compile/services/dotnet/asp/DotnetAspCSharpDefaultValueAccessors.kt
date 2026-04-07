package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request.DotnetAspDefaultValue

internal fun DotnetAspDefaultValue.requireString(): String = when (this) {
    is DotnetAspDefaultValue.StringValue -> value

    is DotnetAspDefaultValue.CharValue,
    is DotnetAspDefaultValue.NumericValue,
    is DotnetAspDefaultValue.BooleanValue,
    is DotnetAspDefaultValue.UuidValue,
    is DotnetAspDefaultValue.LocalDateValue,
    is DotnetAspDefaultValue.LocalTimeValue,
    is DotnetAspDefaultValue.LocalDateTimeValue,
    is DotnetAspDefaultValue.InstantValue,
    is DotnetAspDefaultValue.OffsetDateTimeValue,
    is DotnetAspDefaultValue.DurationValue,
    -> invalidAspDefaultValue("string", this)
}

internal fun DotnetAspDefaultValue.requireChar(): Char = when (this) {
    is DotnetAspDefaultValue.CharValue -> value

    is DotnetAspDefaultValue.StringValue,
    is DotnetAspDefaultValue.NumericValue,
    is DotnetAspDefaultValue.BooleanValue,
    is DotnetAspDefaultValue.UuidValue,
    is DotnetAspDefaultValue.LocalDateValue,
    is DotnetAspDefaultValue.LocalTimeValue,
    is DotnetAspDefaultValue.LocalDateTimeValue,
    is DotnetAspDefaultValue.InstantValue,
    is DotnetAspDefaultValue.OffsetDateTimeValue,
    is DotnetAspDefaultValue.DurationValue,
    -> invalidAspDefaultValue("char", this)
}

internal fun DotnetAspDefaultValue.requireNumber(): Number = when (this) {
    is DotnetAspDefaultValue.NumericValue -> value

    is DotnetAspDefaultValue.StringValue,
    is DotnetAspDefaultValue.CharValue,
    is DotnetAspDefaultValue.BooleanValue,
    is DotnetAspDefaultValue.UuidValue,
    is DotnetAspDefaultValue.LocalDateValue,
    is DotnetAspDefaultValue.LocalTimeValue,
    is DotnetAspDefaultValue.LocalDateTimeValue,
    is DotnetAspDefaultValue.InstantValue,
    is DotnetAspDefaultValue.OffsetDateTimeValue,
    is DotnetAspDefaultValue.DurationValue,
    -> invalidAspDefaultValue("numeric", this)
}

internal fun DotnetAspDefaultValue.requireBoolean(): Boolean = when (this) {
    is DotnetAspDefaultValue.BooleanValue -> value

    is DotnetAspDefaultValue.StringValue,
    is DotnetAspDefaultValue.CharValue,
    is DotnetAspDefaultValue.NumericValue,
    is DotnetAspDefaultValue.UuidValue,
    is DotnetAspDefaultValue.LocalDateValue,
    is DotnetAspDefaultValue.LocalTimeValue,
    is DotnetAspDefaultValue.LocalDateTimeValue,
    is DotnetAspDefaultValue.InstantValue,
    is DotnetAspDefaultValue.OffsetDateTimeValue,
    is DotnetAspDefaultValue.DurationValue,
    -> invalidAspDefaultValue("boolean", this)
}

private fun invalidAspDefaultValue(expected: String, actual: DotnetAspDefaultValue): Nothing {
    error("Expected a $expected ASP.NET default value, but was ${actual::class.simpleName}.")
}
