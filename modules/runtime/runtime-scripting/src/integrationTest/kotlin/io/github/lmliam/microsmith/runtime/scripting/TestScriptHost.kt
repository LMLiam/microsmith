package io.github.lmliam.microsmith.runtime.scripting

import io.github.lmliam.microsmith.gen.composition.createMicrosmithGenerator
import io.github.lmliam.microsmith.gen.system.SystemConsole
import io.github.lmliam.microsmith.gen.system.SystemEnvironment
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.time.Clock
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun testScriptHost(
    diagnostics: DiagnosticSink = RecordingDiagnosticSink(),
    timeSource: TimeSource = TimeSource.Monotonic,
    cacheDirectory: Path? = null,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): MicrosmithScriptHost {
    val fileSystem = FileSystems.getDefault()
    val environment = SystemEnvironment()

    return createMicrosmithScriptHost(
        generator =
            createMicrosmithGenerator(
                fileSystem = fileSystem,
                clock = Clock.System,
                environment = environment,
                console = SystemConsole(),
                diagnostics = diagnostics,
                ioDispatcher = dispatcher,
            ),
        fileSystem = fileSystem,
        environment = environment,
        diagnostics = diagnostics,
        timeSource = timeSource,
        cacheDirectory = cacheDirectory,
    )
}
