package me.liam.microsmith.cli.parsing

import me.liam.microsmith.cli.diagnostics.DiagnosticFormat

internal class DiagnosticOptionsState {
    var diagnosticsFormat: DiagnosticFormat = DiagnosticFormat.TEXT
        private set
    var verbose: Boolean = false
        private set

    private var diagnosticsSpecified = false

    var error: String? = null
        private set

    fun consumeDiagnostics(args: List<String>, index: Int): Int {
        val value = args.getOrNull(index + 1)
        val parsedFormat = parseDiagnosticFormat(value)
        if (value == null || value.startsWith("--")) {
            error = "Missing value for --diagnostics option."
            return 0
        }
        if (diagnosticsSpecified) {
            error = "--diagnostics may only be specified once."
            return 0
        }
        if (parsedFormat == null) {
            error = "Invalid --diagnostics value '$value'. Expected 'text' or 'json'."
            return 0
        }

        diagnosticsFormat = parsedFormat
        diagnosticsSpecified = true
        return 2
    }

    fun consumeVerbose(): Int {
        if (verbose) {
            error = "--verbose may only be specified once."
            return 0
        }

        verbose = true
        return 1
    }

    fun consumeUnknownOption(token: String) {
        error = "Unknown option '$token'."
    }
}
