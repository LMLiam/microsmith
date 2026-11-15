package io.github.lmliam.microsmith.runtime.scripting.composition

import dev.zacsweers.metro.Binds
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.gen.MicrosmithGenerator
import io.github.lmliam.microsmith.plugin.DiagnosticSink
import io.github.lmliam.microsmith.plugin.Environment
import io.github.lmliam.microsmith.runtime.scripting.MicrosmithScriptHost
import io.github.lmliam.microsmith.runtime.scripting.host.isolation.process.JvmProcessIsolationWorkerLauncher
import io.github.lmliam.microsmith.runtime.scripting.host.isolation.process.ProcessIsolationWorkerLauncher
import io.github.lmliam.microsmith.runtime.scripting.host.paths.ScriptCacheDirectoryPolicy
import java.nio.file.FileSystem
import java.nio.file.Path
import kotlin.time.TimeSource

@DependencyGraph(ScriptHostScope::class)
internal interface ScriptHostGraph : ScriptRunGraph.Factory {
    val host: MicrosmithScriptHost

    @Binds val JvmProcessIsolationWorkerLauncher.bindWorkerLauncher: ProcessIsolationWorkerLauncher

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides generator: MicrosmithGenerator,
            @Provides fileSystem: FileSystem,
            @Provides environment: Environment,
            @Provides diagnostics: DiagnosticSink,
            @Provides timeSource: TimeSource,
            @Provides configuredCacheDirectory: Path?,
        ): ScriptHostGraph
    }

    @Provides
    @SingleIn(ScriptHostScope::class)
    fun provideCacheDirectory(policy: ScriptCacheDirectoryPolicy): Path = policy.resolve()
}
