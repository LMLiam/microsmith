package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.plugin.Console
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink

internal class ConsoleDiagnosticSink(private val console: Console) : DiagnosticSink {
    override fun report(diagnostic: Diagnostic) {
        val line = diagnostic.pluginId?.let { "[$it] ${diagnostic.message}" } ?: diagnostic.message

        when (diagnostic.severity) {
            DiagnosticSeverity.INFO -> console.out(line)
            DiagnosticSeverity.WARNING -> console.err(line)
        }
    }
}
