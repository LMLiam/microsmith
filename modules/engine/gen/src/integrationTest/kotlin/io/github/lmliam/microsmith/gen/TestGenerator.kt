package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.gen.composition.createMicrosmithGenerator
import io.github.lmliam.microsmith.gen.system.SystemConsole
import io.github.lmliam.microsmith.gen.system.SystemEnvironment
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import java.nio.file.FileSystems
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

fun testGenerator(diagnostics: DiagnosticSink, dispatcher: CoroutineDispatcher = Dispatchers.IO): MicrosmithGenerator =
    createMicrosmithGenerator(
        fileSystem = FileSystems.getDefault(),
        clock = Clock.System,
        environment = SystemEnvironment(),
        console = SystemConsole(),
        diagnostics = diagnostics,
        ioDispatcher = dispatcher,
    )
