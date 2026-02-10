package me.liam.microsmith.runtime.scripting

import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.with
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.jvm

internal object ScriptHostConfigurationFactory {
    fun create(
        cache: MicrosmithScriptCache,
        runtimeClassLoader: ClassLoader
    ): ScriptingHostConfiguration =
        defaultJvmScriptingHostConfiguration.with {
            jvm {
                baseClassLoader(runtimeClassLoader)
                compilationCache(cache)
            }
        }
}
