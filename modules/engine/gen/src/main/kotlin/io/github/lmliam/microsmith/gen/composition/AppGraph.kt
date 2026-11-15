package io.github.lmliam.microsmith.gen.composition

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import io.github.lmliam.microsmith.gen.MicrosmithGenerator
import io.github.lmliam.microsmith.gen.RunGraphMicrosmithGenerator
import io.github.lmliam.microsmith.plugin.Console
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import java.nio.file.FileSystem
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineDispatcher

@DependencyGraph(AppScope::class)
internal interface AppGraph : RunGraph.Factory {
    val generator: MicrosmithGenerator

    @Binds val RunGraphMicrosmithGenerator.bindGenerator: MicrosmithGenerator

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides fileSystem: FileSystem,
            @Provides clock: Clock,
            @Provides environment: Environment,
            @Provides console: Console,
            @Provides diagnostics: DiagnosticSink,
            @Provides ioDispatcher: CoroutineDispatcher,
        ): AppGraph
    }
}
