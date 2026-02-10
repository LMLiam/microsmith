package me.liam.microsmith.runtime.scripting.definition

import me.liam.microsmith.dsl.core.MicrosmithScope
import me.liam.microsmith.dsl.core.microsmith
import me.liam.microsmith.dsl.schemas.core.SchemasScope
import me.liam.microsmith.dsl.schemas.core.schemas
import me.liam.microsmith.dsl.schemas.protobuf.ProtobufScope
import me.liam.microsmith.dsl.schemas.protobuf.protobuf
import me.liam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader

object MicrosmithScriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(
            importFromPackageOf(MicrosmithScope::class, ::microsmith),
            importFromPackageOf(SchemasScope::class, MicrosmithScope::schemas),
            importFromPackageOf(ProtobufScope::class, SchemasScope::protobuf)
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

private fun importFromPackageOf(
    owner: KClass<*>,
    symbol: KFunction<*>
): String = "${owner.java.packageName}.${symbol.name}"

