package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.util.concurrent.ConcurrentLinkedQueue
import thirdparty.contract.CLOSED_CODE

class RecordingDiagnosticSink : DiagnosticSink {
    private val recorded = ConcurrentLinkedQueue<Diagnostic>()

    val diagnostics: List<Diagnostic>
        get() = recorded.toList()

    fun closedPluginIds(): List<String?> = diagnostics.filter { it.code == CLOSED_CODE }.map(Diagnostic::pluginId)

    fun warnings(): List<Diagnostic> = diagnostics.filter { it.severity == DiagnosticSeverity.WARNING }

    override fun report(diagnostic: Diagnostic) {
        recorded.add(diagnostic)
    }
}
