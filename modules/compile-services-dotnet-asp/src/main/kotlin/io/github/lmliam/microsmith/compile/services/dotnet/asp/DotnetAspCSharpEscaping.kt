package io.github.lmliam.microsmith.compile.services.dotnet.asp

private const val FIRST_PRINTABLE_ASCII_CODE_POINT = 0x20
private const val UNICODE_ESCAPE_WIDTH = 4

internal fun dotnetAspEscapeStringContents(value: String): String = buildString {
    value.forEach { char ->
        append(dotnetAspEscapedCharacter(char))
    }
}

internal fun dotnetAspStringLiteral(value: String): String = buildString {
    append('"')
    append(dotnetAspEscapeStringContents(value))
    append('"')
}

internal fun dotnetAspCharLiteral(value: Char): String {
    val char = value
    val escaped = when (char) {
        '\\' -> "\\\\"
        '\'' -> "\\'"
        '\b' -> "\\b"
        '\u000C' -> "\\f"
        '\n' -> "\\n"
        '\r' -> "\\r"
        '\t' -> "\\t"
        else -> char.toString()
    }
    return "'$escaped'"
}

private fun dotnetAspEscapedCharacter(char: Char): String = when (char) {
    '\\' -> "\\\\"
    '"' -> "\\\""
    '\b' -> "\\b"
    '\u000C' -> "\\f"
    '\n' -> "\\n"
    '\r' -> "\\r"
    '\t' -> "\\t"
    else ->
        if (char.code < FIRST_PRINTABLE_ASCII_CODE_POINT) {
            "\\u${char.code.toString(radix = 16).padStart(UNICODE_ESCAPE_WIDTH, '0')}"
        } else {
            char.toString()
        }
}
