package io.github.lmliam.microsmith.gen

import io.github.lmliam.microsmith.dsl.MicrosmithModel
import io.github.lmliam.microsmith.gen.composition.createMicrosmithGenerator
import io.github.lmliam.microsmith.gen.files.FileSpace
import io.github.lmliam.microsmith.gen.system.ConsoleDiagnosticSink
import io.github.lmliam.microsmith.gen.system.SystemConsole
import io.github.lmliam.microsmith.gen.system.SystemEnvironment
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers

@Suppress("InjectDispatcher")
private val systemGenerator: MicrosmithGenerator by lazy {
    val console = SystemConsole()

    createMicrosmithGenerator(
        fileSystem = FileSystems.getDefault(),
        clock = Clock.System,
        environment = SystemEnvironment(),
        console = console,
        diagnostics = ConsoleDiagnosticSink(console),
        ioDispatcher = Dispatchers.IO,
    )
}

public suspend fun MicrosmithModel.generate(
    finalDir: FileSpace,
    pluginClassLoader: ClassLoader = MicrosmithModel::class.java.classLoader,
): List<Path> = systemGenerator.generate(this, finalDir, pluginClassLoader)

public suspend fun MicrosmithModel.generateTo(
    outputDir: Path,
    pluginClassLoader: ClassLoader = MicrosmithModel::class.java.classLoader,
): List<Path> = systemGenerator.generateTo(this, outputDir, pluginClassLoader)
