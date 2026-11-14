package io.github.lmliam.microsmith.gen.plugins

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSink

internal class PluginDiagnosticSink(private val pluginId: String, private val delegate: DiagnosticSink) :
    DiagnosticSink {
    override fun report(diagnostic: Diagnostic) {
        delegate.report(Diagnostic(diagnostic.severity, diagnostic.code, diagnostic.message, pluginId))
    }
}
