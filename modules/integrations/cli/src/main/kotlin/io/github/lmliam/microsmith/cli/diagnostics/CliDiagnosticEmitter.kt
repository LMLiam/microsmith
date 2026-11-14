package io.github.lmliam.microsmith.cli.diagnostics

import java.time.Instant

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

    fun error(
        code: CliFailureCode,
        message: String,
        details: Map<String, String> = emptyMap(),
        issueCode: String? = null,
    ) {
        emit(
            sink = stderr,
            event =
                DiagnosticEvent(
                    level = DiagnosticLevel.ERROR,
                    code = code.id,
                    issueCode = issueCode,
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
        val issueCode = event.issueCode?.let { "[$it] " }.orEmpty()
        sink("$prefix $code$issueCode${event.message}")
        if (verbose) {
            event.details.toSortedMap().forEach { (key, value) -> sink("  $key=$value") }
        }
    }

    private fun emitJson(sink: (String) -> Unit, event: DiagnosticEvent) {
        val payload =
            linkedMapOf<String, Any?>(
                    "timestamp" to Instant.now().toString(),
                    "level" to event.level.name.lowercase(),
                    "message" to event.message,
                )
                .apply {
                    event.code?.let { put("code", it) }
                    event.issueCode?.let { put("issueCode", it) }
                    if (verbose && event.details.isNotEmpty()) {
                        put("details", event.details.toSortedMap())
                    }
                }
        sink(toJsonValue(payload))
    }
}
