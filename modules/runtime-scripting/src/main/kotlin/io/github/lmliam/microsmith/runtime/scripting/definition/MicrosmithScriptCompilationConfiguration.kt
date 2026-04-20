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
            "io.github.lmliam.microsmith.dsl.core.microsmith",
            "io.github.lmliam.microsmith.dsl.services.core.services",
            "io.github.lmliam.microsmith.dsl.services.dotnet.core.dotnet",
            "io.github.lmliam.microsmith.dsl.services.dotnet.core.service.asp",
            "io.github.lmliam.microsmith.dsl.services.dotnet.core.service.aspNet",
            "io.github.lmliam.microsmith.dsl.services.dotnet.core.service.packages",
            "io.github.lmliam.microsmith.dsl.services.dotnet.core.solution.packages",
            "io.github.lmliam.microsmith.dsl.schemas.core.schemas",
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
