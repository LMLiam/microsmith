package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.util.concurrent.ConcurrentLinkedQueue

class RecordingDiagnosticSink : DiagnosticSink {
    private val recorded = ConcurrentLinkedQueue<Diagnostic>()

    val diagnostics: List<Diagnostic>
        get() = recorded.toList()

    override fun report(diagnostic: Diagnostic) {
        recorded += diagnostic
    }
}
