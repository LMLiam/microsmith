package io.github.lmliam.microsmith.gen.composition

import dev.zacsweers.metro.createGraphFactory
import io.github.lmliam.microsmith.gen.MicrosmithGenerator
import io.github.lmliam.microsmith.plugin.Console
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import java.nio.file.FileSystem
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher

fun createMicrosmithGenerator(
    fileSystem: FileSystem,
    clock: Clock,
    environment: Environment,
    console: Console,
    diagnostics: DiagnosticSink,
    ioDispatcher: CoroutineDispatcher,
): MicrosmithGenerator =
    createGraphFactory<AppGraph.Factory>()
        .create(fileSystem, clock, environment, console, diagnostics, ioDispatcher)
        .generator
