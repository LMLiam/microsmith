package io.github.lmliam.microsmith.gen.files

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import java.nio.file.FileSystem
import java.nio.file.Files
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Inject
internal class TemporaryDirectories(
    private val fileSystem: FileSystem,
    private val clock: Clock,
    private val environment: Environment,
    private val diagnostics: DiagnosticSink,
    private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun create(): TemporaryDirectory =
        withContext(ioDispatcher) {
            val parent = fileSystem.getPath(checkNotNull(environment.property("java.io.tmpdir")))
            val root = Files.createTempDirectory(parent, PREFIX + clock.now().toEpochMilliseconds())

            TemporaryDirectory(root, diagnostics)
        }

    private companion object {
        const val PREFIX = ".microsmith-gen-temp-"
    }
}
