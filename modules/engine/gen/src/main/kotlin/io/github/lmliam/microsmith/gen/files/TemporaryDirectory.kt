package io.github.lmliam.microsmith.gen.files

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.io.Closeable
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

internal class TemporaryDirectory(override val root: Path, private val diagnostics: DiagnosticSink) :
    FileSpace, Closeable {
    override fun close() {
        runCatching {
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
            }
        }
            .onFailure { error ->
                diagnostics.report(
                    Diagnostic(
                        severity = DiagnosticSeverity.WARNING,
                        code = "gen.temporary-directory-cleanup-failed",
                        message = "Failed to cleanup temporary directory '$root': ${error.message}",
                    ),
                )
            }
    }
}
