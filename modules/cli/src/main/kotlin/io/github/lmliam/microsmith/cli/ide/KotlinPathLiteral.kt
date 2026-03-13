package io.github.lmliam.microsmith.cli.ide

import java.nio.file.Path

internal fun Path.toKotlinPathLiteral(): String = toAbsolutePath()
    .normalize()
    .toString()
    .replace('\\', '/')
    .toKotlinStringLiteralContent()

private fun String.toKotlinStringLiteralContent(): String {
    val builder = StringBuilder(length + KOTLIN_ESCAPE_BUFFER_PADDING)
    for (char in this) {
        when (char) {
            '$' -> builder.append("\\$")
            '"' -> builder.append("\\\"")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            else -> builder.append(char)
        }
    }
    return builder.toString()
}

private const val KOTLIN_ESCAPE_BUFFER_PADDING = 8
