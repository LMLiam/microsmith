package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.gen.composition.createMicrosmithGenerator
import io.github.lmliam.microsmith.gen.system.SystemConsole
import io.github.lmliam.microsmith.gen.system.SystemEnvironment
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import java.nio.file.FileSystems
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun testGenerator(
    diagnostics: DiagnosticSink,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    environment: Environment = SystemEnvironment(),
): MicrosmithGenerator =
    createMicrosmithGenerator(
        fileSystem = FileSystems.getDefault(),
        clock = Clock.System,
        environment = environment,
        console = SystemConsole(),
        diagnostics = diagnostics,
        ioDispatcher = dispatcher,
    )
