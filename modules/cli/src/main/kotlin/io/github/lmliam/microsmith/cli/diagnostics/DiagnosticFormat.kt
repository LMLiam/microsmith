package io.github.lmliam.microsmith.cli.diagnostics

internal enum class DiagnosticFormat(val cliValue: String) {
    TEXT("text"),
    JSON("json"),
    ;

    companion object {
        fun parse(value: String?): DiagnosticFormat? = entries.firstOrNull { it.cliValue == value }
    }
}
