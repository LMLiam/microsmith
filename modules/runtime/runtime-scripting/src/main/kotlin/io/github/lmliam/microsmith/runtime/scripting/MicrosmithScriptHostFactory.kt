package io.github.lmliam.microsmith.runtime.scripting

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.InternalMicrosmithApi
import io.github.lmliam.microsmith.gen.MicrosmithGenerator
import io.github.lmliam.microsmith.gen.composition.createMicrosmithGenerator
import io.github.lmliam.microsmith.gen.system.ConsoleDiagnosticSink
import io.github.lmliam.microsmith.gen.system.SystemConsole
import io.github.lmliam.microsmith.gen.system.SystemEnvironment
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import io.github.lmliam.microsmith.runtime.scripting.composition.ScriptHostGraph
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.time.Clock
import kotlin.time.TimeSource
import kotlinx.coroutines.Dispatchers

fun createMicrosmithScriptHost(
    generator: MicrosmithGenerator,
    fileSystem: FileSystem,
    environment: Environment,
    diagnostics: DiagnosticSink,
    timeSource: TimeSource,
    cacheDirectory: Path? = null,
): MicrosmithScriptHost =
    createGraphFactory<ScriptHostGraph.Factory>()
        .create(generator, fileSystem, environment, diagnostics, timeSource, cacheDirectory)
        .host

@InternalMicrosmithApi
@Suppress("InjectDispatcher")
fun createSystemMicrosmithScriptHost(cacheDirectory: Path? = null): MicrosmithScriptHost {
    val console = SystemConsole()
    val environment = SystemEnvironment()
    val diagnostics = ConsoleDiagnosticSink(console)
    val fileSystem = FileSystems.getDefault()

    return createMicrosmithScriptHost(
        generator =
            createMicrosmithGenerator(
                fileSystem = fileSystem,
                clock = Clock.System,
                environment = environment,
                console = console,
                diagnostics = diagnostics,
                ioDispatcher = Dispatchers.IO,
            ),
        fileSystem = fileSystem,
        environment = environment,
        diagnostics = diagnostics,
        timeSource = TimeSource.Monotonic,
        cacheDirectory = cacheDirectory,
    )
}
