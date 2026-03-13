package io.github.lmliam.microsmith.cli.diagnostics

internal data class DiagnosticEvent(
    val level: DiagnosticLevel,
    val message: String,
    val code: String? = null,
    val details: Map<String, String> = emptyMap(),
)
