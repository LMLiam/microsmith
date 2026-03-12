package me.liam.microsmith.cli.diagnostics

internal fun toJsonValue(value: Any?): String = when (value) {
    null -> "null"
    is String -> "\"${value.escapeJson()}\""
    is Number -> value.toString()
    is Boolean -> value.toString()
    is Map<*, *> ->
        value.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
        ) { (key, mapValue) ->
            "\"${key.toString().escapeJson()}\":${toJsonValue(mapValue)}"
        }

    is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]", separator = ",") { entry -> toJsonValue(entry) }
    else -> "\"${value.toString().escapeJson()}\""
}

private fun String.escapeJson(): String {
    val builder = StringBuilder(length + JSON_ESCAPE_BUFFER_PADDING)
    for (char in this) {
        when (char) {
            '\\' -> builder.append("\\\\")
            '"' -> builder.append("\\\"")
            '\b' -> builder.append("\\b")
            '\u000C' -> builder.append("\\f")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            else -> {
                if (char.code <= MAX_JSON_CONTROL_CHAR_CODE) {
                    builder.append("\\u%04x".format(char.code))
                } else {
                    builder.append(char)
                }
            }
        }
    }
    return builder.toString()
}

private const val MAX_JSON_CONTROL_CHAR_CODE = 0x1F
private const val JSON_ESCAPE_BUFFER_PADDING = 8
