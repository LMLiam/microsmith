package io.github.lmliam.microsmith.dsl.services.dotnet.asp.core.rest.request

import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

sealed interface DotnetAspDefaultValue {
    @JvmInline
    value class StringValue(val value: String) : DotnetAspDefaultValue

    @JvmInline
    value class CharValue(val value: Char) : DotnetAspDefaultValue

    @JvmInline
    value class NumericValue(val value: Number) : DotnetAspDefaultValue

    @JvmInline
    value class BooleanValue(val value: Boolean) : DotnetAspDefaultValue

    @JvmInline
    value class UuidValue(val value: UUID) : DotnetAspDefaultValue

    @JvmInline
    value class LocalDateValue(val value: LocalDate) : DotnetAspDefaultValue

    @JvmInline
    value class LocalTimeValue(val value: LocalTime) : DotnetAspDefaultValue

    @JvmInline
    value class LocalDateTimeValue(val value: LocalDateTime) : DotnetAspDefaultValue

    @JvmInline
    value class InstantValue(val value: Instant) : DotnetAspDefaultValue

    @JvmInline
    value class OffsetDateTimeValue(val value: OffsetDateTime) : DotnetAspDefaultValue

    @JvmInline
    value class DurationValue(val value: Duration) : DotnetAspDefaultValue
}

internal fun dotnetAspDefaultValue(value: Any): DotnetAspDefaultValue = when (value) {
    is String -> DotnetAspDefaultValue.StringValue(value)

    is Char -> DotnetAspDefaultValue.CharValue(value)

    is Number -> DotnetAspDefaultValue.NumericValue(value)

    is Boolean -> DotnetAspDefaultValue.BooleanValue(value)

    is UUID -> DotnetAspDefaultValue.UuidValue(value)

    is LocalDate -> DotnetAspDefaultValue.LocalDateValue(value)

    is LocalTime -> DotnetAspDefaultValue.LocalTimeValue(value)

    is LocalDateTime -> DotnetAspDefaultValue.LocalDateTimeValue(value)

    is Instant -> DotnetAspDefaultValue.InstantValue(value)

    is OffsetDateTime -> DotnetAspDefaultValue.OffsetDateTimeValue(value)

    is Duration -> DotnetAspDefaultValue.DurationValue(value)

    else ->
        error(
            "Unsupported ASP.NET request default value type " +
                "'${value::class.qualifiedName ?: value::class}'.",
        )
}

internal fun requireCompatibleDotnetAspDefaultValue(
    type: DotnetFieldType,
    defaultValue: DotnetAspDefaultValue?,
): DotnetAspDefaultValue? {
    if (defaultValue == null) {
        return null
    }

    val compatible =
        when {
            type == DotnetFieldType.String -> defaultValue is DotnetAspDefaultValue.StringValue
            type == DotnetFieldType.Char -> defaultValue is DotnetAspDefaultValue.CharValue
            type.isNumeric() -> defaultValue is DotnetAspDefaultValue.NumericValue
            type == DotnetFieldType.Bool -> defaultValue is DotnetAspDefaultValue.BooleanValue
            type == DotnetFieldType.Guid -> defaultValue.isGuidCompatible()
            type == DotnetFieldType.DateOnly -> defaultValue.isDateOnlyCompatible()
            type == DotnetFieldType.TimeOnly -> defaultValue.isTimeOnlyCompatible()
            type == DotnetFieldType.DateTime -> defaultValue.isDateTimeCompatible()
            type == DotnetFieldType.DateTimeOffset -> defaultValue.isDateTimeOffsetCompatible()
            type == DotnetFieldType.TimeSpan -> defaultValue.isTimeSpanCompatible()
            type is DotnetFieldType.Reference -> false
            else -> false
        }

    require(compatible) {
        "ASP.NET request field defaults for type '$type' cannot use " +
            defaultValue::class.simpleName.orEmpty().ifBlank { "the provided value type" } +
            "."
    }

    return defaultValue
}

private fun DotnetFieldType.isNumeric(): Boolean = when (this) {
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
    -> true

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
    -> false
}

private fun DotnetAspDefaultValue.isGuidCompatible(): Boolean =
    this is DotnetAspDefaultValue.StringValue || this is DotnetAspDefaultValue.UuidValue

private fun DotnetAspDefaultValue.isDateOnlyCompatible(): Boolean =
    this is DotnetAspDefaultValue.StringValue || this is DotnetAspDefaultValue.LocalDateValue

private fun DotnetAspDefaultValue.isTimeOnlyCompatible(): Boolean =
    this is DotnetAspDefaultValue.StringValue || this is DotnetAspDefaultValue.LocalTimeValue

private fun DotnetAspDefaultValue.isDateTimeCompatible(): Boolean = this is DotnetAspDefaultValue.StringValue ||
    this is DotnetAspDefaultValue.LocalDateTimeValue ||
    this is DotnetAspDefaultValue.InstantValue

private fun DotnetAspDefaultValue.isDateTimeOffsetCompatible(): Boolean = this is DotnetAspDefaultValue.StringValue ||
    this is DotnetAspDefaultValue.OffsetDateTimeValue ||
    this is DotnetAspDefaultValue.InstantValue

private fun DotnetAspDefaultValue.isTimeSpanCompatible(): Boolean =
    this is DotnetAspDefaultValue.StringValue || this is DotnetAspDefaultValue.DurationValue
