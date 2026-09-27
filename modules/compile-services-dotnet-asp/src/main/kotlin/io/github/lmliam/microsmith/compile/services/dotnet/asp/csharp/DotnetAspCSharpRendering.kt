package io.github.lmliam.microsmith.compile.services.dotnet.asp.csharp

import io.github.lmliam.microsmith.artifact.services.dotnet.asp.request.DotnetAspRequestFieldArtifact
import io.github.lmliam.microsmith.dsl.services.dotnet.model.DotnetFieldType

internal fun renderDotnetAspModelPropertyType(type: DotnetFieldType): String =
    if (type is DotnetFieldType.Reference) type.target else type.csharpType

internal fun renderDotnetAspBindingPropertyType(field: DotnetAspRequestFieldArtifact): String {
    val baseType = renderDotnetAspModelPropertyType(field.type)
    return if (field.optional && field.defaultValue == null) "$baseType?" else baseType
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
                if (char.code < FIRST_NON_PRINTABLE_ASCII_CODE_POINT) {
                    append(char.toUnicodeEscape())
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
    else -> renderDotnetAspPrintableCharLiteral(value)
}

private fun renderDotnetAspPrintableCharLiteral(value: Char): String =
    if (value.code < FIRST_NON_PRINTABLE_ASCII_CODE_POINT) {
        "'${value.toUnicodeEscape()}'"
    } else {
        "'$value'"
    }

private const val FIRST_NON_PRINTABLE_ASCII_CODE_POINT = 0x20

private fun Char.toUnicodeEscape(): String = "\\u${code.toString(radix = 16).padStart(length = 4, padChar = '0')}"
