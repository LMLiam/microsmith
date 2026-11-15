package io.github.lmliam.microsmith.gen.files

import io.github.lmliam.microsmith.plugin.Diagnostic
import io.github.lmliam.microsmith.plugin.DiagnosticSeverity
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.io.Closeable
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.deleteIfExists
import kotlin.io.path.notExists
import kotlin.io.path.walk

internal class TemporaryDirectory(override val root: Path, private val diagnostics: DiagnosticSink) :
    FileSpace, Closeable {
    @OptIn(ExperimentalPathApi::class)
    override fun close() {
        runCatching {
            if (root.notExists()) throw NoSuchFileException(root.toString())
            root.walk(PathWalkOption.INCLUDE_DIRECTORIES).sortedDescending().forEach { it.deleteIfExists() }
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
