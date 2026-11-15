package io.github.lmliam.microsmith.runtime.scripting.host.configuration

import dev.zacsweers.metro.Inject
import io.github.lmliam.microsmith.runtime.scripting.cache.MicrosmithScriptCache
import io.github.lmliam.microsmith.runtime.scripting.host.execution.ScriptRunClassLoader
import io.github.lmliam.microsmith.runtime.scripting.symbols.discovery.ScriptSymbolContributorRegistry
import io.github.lmliam.microsmith.runtime.scripting.symbols.refinement.microsmithScriptSymbolRegistry
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm

@Inject
internal class ScriptHostConfigurationFactory(
    private val cache: MicrosmithScriptCache,
    private val classLoader: ScriptRunClassLoader,
    private val symbolRegistry: ScriptSymbolContributorRegistry,
) {
    fun create(): ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration.with {
        microsmithScriptSymbolRegistry(symbolRegistry)
        jvm {
            baseClassLoader(classLoader.loader)
            compilationCache(cache)
        }
    }
}
