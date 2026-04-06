package io.github.lmliam.microsmith.runtime.scripting.definition

import io.github.lmliam.microsmith.dsl.core.MicrosmithScope
import io.github.lmliam.microsmith.dsl.core.microsmith
import io.github.lmliam.microsmith.dsl.schemas.core.SchemasScope
import io.github.lmliam.microsmith.dsl.schemas.core.schemas
import io.github.lmliam.microsmith.dsl.schemas.protobuf.ProtobufScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.ServiceScope
import io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service
import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
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
            importFromPackageOf(ProtobufScope::class, SchemasScope::protobuf),
            importFromPackageOf(ServiceScope::class, ProtobufScope::service),
        )

        implicitReceivers(MicrosmithScriptContext::class)

        jvm {
            updateClasspath(
                classpathFromClassloader(
                    MicrosmithScript::class.java.classLoader,
                    unpackJarCollections = true,
                ).orEmpty(),
            )
        }
    },
)

private fun importFromPackageOf(owner: KClass<*>, symbol: KFunction<*>): String =
    "${owner.java.packageName}.${symbol.name}"
