package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.util.Locale

internal fun renderDotnetAspModelPropertyType(type: DotnetFieldType): String = when (type) {
    is DotnetFieldType.Reference -> type.target
    else -> type.csharpType
}

internal fun renderDotnetAspBindingPropertyType(field: DotnetAspRequestFieldArtifact): String {
    val baseType = renderDotnetAspModelPropertyType(field.type)
    return if (field.optional && field.defaultValue == null) "$baseType?" else baseType
}

internal fun renderDotnetAspInitializer(type: DotnetFieldType): String = when (type) {
    DotnetFieldType.String -> " = string.Empty;"
    DotnetFieldType.Char -> " = 'A';"
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
    -> " = 0;"
    DotnetFieldType.Float -> " = 0F;"
    DotnetFieldType.Double -> " = 0D;"
    DotnetFieldType.Decimal -> " = 0M;"
    DotnetFieldType.Bool -> " = false;"
    DotnetFieldType.Guid -> " = Guid.Empty;"
    DotnetFieldType.DateOnly -> " = DateOnly.MinValue;"
    DotnetFieldType.TimeOnly -> " = TimeOnly.MinValue;"
    DotnetFieldType.DateTime -> " = DateTime.UnixEpoch;"
    DotnetFieldType.DateTimeOffset -> " = DateTimeOffset.UnixEpoch;"
    DotnetFieldType.TimeSpan -> " = TimeSpan.Zero;"
    is DotnetFieldType.Reference -> " = null!;"
}

internal fun renderDotnetAspBindingInitializer(field: DotnetAspRequestFieldArtifact): String {
    val defaultValue = field.defaultValue
    return when {
        defaultValue != null -> " = ${renderDotnetAspDefaultExpression(field.type, defaultValue)};"
        field.optional -> " = null;"
        else -> renderDotnetAspInitializer(field.type)
    }
}

internal fun renderDotnetAspDefaultExpression(type: DotnetFieldType, defaultValue: Any): String = when (type) {
    DotnetFieldType.String -> escapeDotnetAspCsharpStringLiteral(defaultValue.toString())
    DotnetFieldType.Char -> escapeDotnetAspCsharpCharLiteral(defaultValue.toString().first())
    DotnetFieldType.Byte,
    DotnetFieldType.SignedByte,
    DotnetFieldType.Short,
    DotnetFieldType.UnsignedShort,
    DotnetFieldType.Int,
    -> defaultValue.toString()
    DotnetFieldType.UnsignedInt -> "${defaultValue}U"
    DotnetFieldType.Long -> "${defaultValue}L"
    DotnetFieldType.UnsignedLong -> "${defaultValue}UL"
    DotnetFieldType.NativeInt -> "(nint)$defaultValue"
    DotnetFieldType.UnsignedNativeInt -> "(nuint)${defaultValue}UL"
    DotnetFieldType.Float -> "${defaultValue.toString().ensureDotnetAspDecimal()}F"
    DotnetFieldType.Double -> "${defaultValue.toString().ensureDotnetAspDecimal()}D"
    DotnetFieldType.Decimal -> "${defaultValue.toString().ensureDotnetAspDecimal()}M"
    DotnetFieldType.Bool -> defaultValue.toString().lowercase(Locale.ROOT)
    DotnetFieldType.Guid -> "Guid.Parse(${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())})"
    DotnetFieldType.DateOnly ->
        "DateOnly.Parse(${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture)"
    DotnetFieldType.TimeOnly ->
        "TimeOnly.Parse(${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture)"
    DotnetFieldType.DateTime ->
        "DateTime.Parse(${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture, global::System.Globalization.DateTimeStyles.RoundtripKind)"
    DotnetFieldType.DateTimeOffset ->
        "DateTimeOffset.Parse(${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture, global::System.Globalization.DateTimeStyles.RoundtripKind)"
    DotnetFieldType.TimeSpan ->
        "TimeSpan.Parse(${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())}, global::System.Globalization.CultureInfo.InvariantCulture)"
    is DotnetFieldType.Reference ->
        "global::System.Text.Json.JsonSerializer.Deserialize<${type.target}>(" +
            "${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())})!"
}

internal fun escapeDotnetAspCsharpStringLiteral(value: String): String = buildString {
    append('"')
    value.forEach { char ->
        when (char) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> {
                if (char.code < 0x20) {
                    append("\\u%04x".format(char.code))
                } else {
                    append(char)
                }
            }
        }
    }
    append('"')
}

internal fun escapeDotnetAspCsharpCharLiteral(value: Char): String = when (value) {
    '\\' -> "'\\\\'"
    '\'' -> "'\\''"
    '\n' -> "'\\n'"
    '\r' -> "'\\r'"
    '\t' -> "'\\t'"
    '\b' -> "'\\b'"
    '\u000C' -> "'\\f'"
    else -> if (value.code < 0x20) "'\\u%04x'".format(value.code) else "'$value'"
}

private fun String.ensureDotnetAspDecimal(): String =
    if (contains('.') || contains('E', ignoreCase = true)) this else "$this.0"
