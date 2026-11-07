package io.github.lmliam.microsmith.runtime.scripting.definition

import io.github.lmliam.microsmith.runtime.scripting.context.MicrosmithScriptContext
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.classpathFromClassloader

object MicrosmithScriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        defaultImports(
            "io.github.lmliam.microsmith.dsl.microsmith",
            "io.github.lmliam.microsmith.dsl.services.services",
            "io.github.lmliam.microsmith.dsl.services.dotnet.dotnet",
            "io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.asp",
            "io.github.lmliam.microsmith.dsl.services.dotnet.asp.service.aspNet",
            "io.github.lmliam.microsmith.dsl.services.dotnet.packages.service.packages",
            "io.github.lmliam.microsmith.dsl.services.dotnet.packages.solution.packages",
            "io.github.lmliam.microsmith.dsl.schemas.schemas",
            "io.github.lmliam.microsmith.dsl.schemas.protobuf.protobuf",
            "io.github.lmliam.microsmith.dsl.schemas.protobuf.rpc.service",
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
