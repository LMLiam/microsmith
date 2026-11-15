package io.github.lmliam.microsmith.gen.system

import io.github.lmliam.microsmith.plugin.Console
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink

class ConsoleDiagnosticSink(private val console: Console) : DiagnosticSink {
    override fun report(diagnostic: Diagnostic) {
        when (diagnostic.severity) {
            DiagnosticSeverity.INFO -> console.out(diagnostic.message)
            DiagnosticSeverity.WARNING -> console.err(diagnostic.message)
        }
    }
}
