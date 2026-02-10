package me.liam.microsmith.runtime.scripting

import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader

object MicrosmithScriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(
            "me.liam.microsmith.dsl.core.microsmith",
            "me.liam.microsmith.dsl.schemas.core.schemas",
            "me.liam.microsmith.dsl.schemas.protobuf.protobuf"
        )

        implicitReceivers(MicrosmithScriptContext::class)

        jvm {
            updateClasspath(
                classpathFromClassloader(
                    MicrosmithScript::class.java.classLoader,
                    unpackJarCollections = true
                ).orEmpty()
            )
        }
    }
) {
    @Suppress("unused")
    private fun readResolve(): Any = MicrosmithScriptCompilationConfiguration
}

