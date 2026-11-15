package io.github.lmliam.microsmith.gen.execution

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink

@Inject
internal class GenerationProgressReporter(private val diagnostics: DiagnosticSink) {
    fun reportModelGenerationComplete(space: FileSpace) {
        diagnostics.report(
            Diagnostic(
                severity = DiagnosticSeverity.INFO,
                code = "gen.generation-complete",
                message = "Generated all files in ${space.root}",
            ),
        )
    }
}
