package me.liam.microsmith.cli.diagnostics

import java.time.Instant

internal enum class DiagnosticFormat(
    val cliValue: String,
) {
    TEXT("text"),
    JSON("json"),
    ;

    companion object {
        fun parse(value: String?): DiagnosticFormat? = entries.firstOrNull { it.cliValue == value }
    }
}

internal enum class CliFailureCode(
    val id: String,
    val exitCode: Int,
) {
    USAGE_ERROR(id = "MS-CLI-0001", exitCode = 2),
    PROVIDER_VALIDATION_FAILED(id = "MS-CLI-1001", exitCode = 10),
    PLUGIN_RESOLUTION_FAILED(id = "MS-CLI-1101", exitCode = 11),
    SCRIPT_VALIDATION_FAILED(id = "MS-CLI-2001", exitCode = 20),
    SCRIPT_COMPILATION_FAILED(id = "MS-CLI-2002", exitCode = 21),
    SCRIPT_EVALUATION_FAILED(id = "MS-CLI-2003", exitCode = 22),
    SCRIPT_HOST_FAILED(id = "MS-CLI-2004", exitCode = 23),
    DOCTOR_FAILED(id = "MS-CLI-3001", exitCode = 30),
}

internal enum class DiagnosticLevel {
    INFO,
    WARN,
    ERROR,
}

internal data class DiagnosticEvent(
    val level: DiagnosticLevel,
    val message: String,
    val code: String? = null,
    val details: Map<String, String> = emptyMap(),
)

internal class CliDiagnosticEmitter(
    private val format: DiagnosticFormat,
    private val verbose: Boolean,
    private val stdout: (String) -> Unit,
    private val stderr: (String) -> Unit,
) {
    fun info(message: String, details: Map<String, String> = emptyMap()) {
        emit(
            sink = stdout,
            event = DiagnosticEvent(level = DiagnosticLevel.INFO, message = message, details = details),
        )
    }

    fun warn(message: String, details: Map<String, String> = emptyMap()) {
        emit(
            sink = stderr,
            event = DiagnosticEvent(level = DiagnosticLevel.WARN, message = message, details = details),
        )
    }

    fun error(code: CliFailureCode, message: String, details: Map<String, String> = emptyMap()) {
        emit(
            sink = stderr,
            event =
            DiagnosticEvent(
                level = DiagnosticLevel.ERROR,
                code = code.id,
                message = message,
                details = details,
            ),
        )
    }

    private fun emit(sink: (String) -> Unit, event: DiagnosticEvent) {
        when (format) {
            DiagnosticFormat.TEXT -> emitText(sink, event)
            DiagnosticFormat.JSON -> emitJson(sink, event)
        }
    }

    private fun emitText(sink: (String) -> Unit, event: DiagnosticEvent) {
        val prefix = "[${event.level.name.lowercase()}]"
        val code = event.code?.let { "[$it] " }.orEmpty()
        sink("$prefix $code${event.message}")
        if (verbose) {
            event.details.toSortedMap().forEach { (key, value) ->
                sink("  $key=$value")
            }
        }
    }

    private fun emitJson(sink: (String) -> Unit, event: DiagnosticEvent) {
        val payload =
            linkedMapOf<String, Any?>(
                "timestamp" to Instant.now().toString(),
                "level" to event.level.name.lowercase(),
                "message" to event.message,
            ).apply {
                event.code?.let { put("code", it) }
                if (verbose && event.details.isNotEmpty()) {
                    put("details", event.details.toSortedMap())
                }
            }
        sink(toJsonValue(payload))
    }
}

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
