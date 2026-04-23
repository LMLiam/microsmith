package io.github.lmliam.microsmith.compile.services.dotnet.asp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.core.model.DotnetFieldType
import java.util.Locale

internal fun renderDotnetAspInitializer(type: DotnetFieldType): String = FIXED_TYPE_INITIALIZERS[type]
    ?: if (type in ZERO_INITIALIZER_TYPES) {
        " = 0;"
    } else {
        " = null!;"
    }

internal fun renderDotnetAspBindingInitializer(field: DotnetAspRequestFieldArtifact): String {
    val defaultValue = field.defaultValue
    return when {
        defaultValue != null -> " = ${renderDotnetAspDefaultExpression(field.type, defaultValue)};"
        field.optional -> " = null;"
        else -> renderDotnetAspInitializer(field.type)
    }
}

internal fun renderDotnetAspDefaultExpression(type: DotnetFieldType, defaultValue: Any): String =
    renderDirectDefaultExpression(type, defaultValue)
        ?: renderNumericDefaultExpression(type, defaultValue)
        ?: renderTemporalDefaultExpression(type, defaultValue)
        ?: renderReferenceDefaultExpression(type, defaultValue)
        ?: error("Unsupported ASP.NET default expression for type '$type'.")

private fun String.ensureDotnetAspDecimal(): String =
    if (contains('.') || contains('E', ignoreCase = true)) this else "$this.0"

private fun renderDirectDefaultExpression(type: DotnetFieldType, defaultValue: Any): String? = when {
    type == DotnetFieldType.String -> escapeDotnetAspCsharpStringLiteral(defaultValue.toString())
    type == DotnetFieldType.Char -> escapeDotnetAspCsharpCharLiteral(defaultValue.toString().first())
    type == DotnetFieldType.Bool -> defaultValue.toString().lowercase(Locale.ROOT)
    type == DotnetFieldType.Guid -> renderParseExpression("Guid", defaultValue)
    type is DotnetFieldType.Reference -> null
    else -> null
}

private fun renderNumericDefaultExpression(type: DotnetFieldType, defaultValue: Any): String? = when {
    type in UNSUFFIXED_NUMERIC_DEFAULT_TYPES -> defaultValue.toString()
    type in SUFFIXED_NUMERIC_DEFAULT_TYPES ->
        "${defaultValue}${SUFFIXED_NUMERIC_DEFAULT_TYPES.getValue(type)}"

    else -> when {
        type == DotnetFieldType.NativeInt -> "(nint)$defaultValue"
        type == DotnetFieldType.UnsignedNativeInt -> "(nuint)${defaultValue}UL"
        type == DotnetFieldType.Float -> "${defaultValue.toString().ensureDotnetAspDecimal()}F"
        type == DotnetFieldType.Double -> "${defaultValue.toString().ensureDotnetAspDecimal()}D"
        type == DotnetFieldType.Decimal -> "${defaultValue.toString().ensureDotnetAspDecimal()}M"
        else -> null
    }
}

private fun renderTemporalDefaultExpression(type: DotnetFieldType, defaultValue: Any): String? = when {
    type == DotnetFieldType.DateOnly -> renderInvariantCultureParse("DateOnly", defaultValue)
    type == DotnetFieldType.TimeOnly -> renderInvariantCultureParse("TimeOnly", defaultValue)
    type == DotnetFieldType.DateTime -> renderRoundtripParse("DateTime", defaultValue)
    type == DotnetFieldType.DateTimeOffset -> renderRoundtripParse("DateTimeOffset", defaultValue)
    type == DotnetFieldType.TimeSpan -> renderInvariantCultureParse("TimeSpan", defaultValue)
    else -> null
}

private fun renderReferenceDefaultExpression(type: DotnetFieldType, defaultValue: Any): String? = when {
    type is DotnetFieldType.Reference ->
        buildString {
            append("global::System.Text.Json.JsonSerializer.Deserialize<")
            append(type.target)
            append(">(")
            append(escapeDotnetAspCsharpStringLiteral(defaultValue.toString()))
            append(")!")
        }

    else -> null
}

private fun renderParseExpression(typeName: String, defaultValue: Any): String =
    "$typeName.Parse(${escapeDotnetAspCsharpStringLiteral(defaultValue.toString())})"

private fun renderInvariantCultureParse(typeName: String, defaultValue: Any): String = buildString {
    append(typeName)
    append(".Parse(")
    append(escapeDotnetAspCsharpStringLiteral(defaultValue.toString()))
    append(", global::System.Globalization.CultureInfo.InvariantCulture)")
}

private fun renderRoundtripParse(typeName: String, defaultValue: Any): String = buildString {
    append(typeName)
    append(".Parse(")
    append(escapeDotnetAspCsharpStringLiteral(defaultValue.toString()))
    append(", global::System.Globalization.CultureInfo.InvariantCulture, ")
    append("global::System.Globalization.DateTimeStyles.RoundtripKind)")
}

private val ZERO_INITIALIZER_TYPES = setOf(
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
)
private val FIXED_TYPE_INITIALIZERS = mapOf(
    DotnetFieldType.String to " = string.Empty;",
    DotnetFieldType.Char to " = 'A';",
    DotnetFieldType.Float to " = 0F;",
    DotnetFieldType.Double to " = 0D;",
    DotnetFieldType.Decimal to " = 0M;",
    DotnetFieldType.Bool to " = false;",
    DotnetFieldType.Guid to " = Guid.Empty;",
    DotnetFieldType.DateOnly to " = DateOnly.MinValue;",
    DotnetFieldType.TimeOnly to " = TimeOnly.MinValue;",
    DotnetFieldType.DateTime to " = DateTime.UnixEpoch;",
    DotnetFieldType.DateTimeOffset to " = DateTimeOffset.UnixEpoch;",
    DotnetFieldType.TimeSpan to " = TimeSpan.Zero;",
)
private val UNSUFFIXED_NUMERIC_DEFAULT_TYPES = setOf(
    DotnetFieldType.Byte,
    DotnetFieldType.SignedByte,
    DotnetFieldType.Short,
    DotnetFieldType.UnsignedShort,
    DotnetFieldType.Int,
)
private val SUFFIXED_NUMERIC_DEFAULT_TYPES = mapOf(
    DotnetFieldType.UnsignedInt to "U",
    DotnetFieldType.Long to "L",
    DotnetFieldType.UnsignedLong to "UL",
)
