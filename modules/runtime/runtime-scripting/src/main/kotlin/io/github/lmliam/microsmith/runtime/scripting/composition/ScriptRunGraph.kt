package io.github.lmliam.microsmith.runtime.scripting.composition

import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import io.github.lmliam.microsmith.runtime.scripting.host.execution.ScriptGenerationBridge
import io.github.lmliam.microsmith.runtime.scripting.host.execution.ScriptRun
import io.github.lmliam.microsmith.runtime.scripting.host.execution.ScriptRunClassLoader
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolContributorRegistry

@GraphExtension(ScriptRunScope::class)
internal interface ScriptRunGraph {
    val classLoader: ScriptRunClassLoader
    val run: ScriptRun

    @GraphExtension.Factory
    fun interface Factory {
        fun create(@Provides request: ScriptRunRequest): ScriptRunGraph
    }

    @Provides
    @SingleIn(ScriptRunScope::class)
    fun provideSymbolRegistry(classLoader: ScriptRunClassLoader): ScriptSymbolContributorRegistry =
        ScriptSymbolContributorRegistry.discover(classLoader.loader)

    @Provides
    @SingleIn(ScriptRunScope::class)
    fun provideScriptContext(request: ScriptRunRequest, bridge: ScriptGenerationBridge): MicrosmithScriptContext =
        MicrosmithScriptContext(
            outDir = request.outputDir,
            vars = request.variables,
            flags = request.flags,
            emitHandler = bridge::generate,
        )
}
