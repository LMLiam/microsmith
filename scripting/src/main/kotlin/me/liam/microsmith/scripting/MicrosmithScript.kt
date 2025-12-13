package me.liam.microsmith.scripting

import me.liam.microsmith.dsl.core.MicrosmithBuilder
import me.liam.microsmith.dsl.core.MicrosmithModel
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.defaultImports
import kotlin.script.experimental.api.implicitReceivers
import kotlin.script.experimental.jvm.dependenciesFromClassloader
import kotlin.script.experimental.jvm.jvm

@KotlinScript(
    fileExtension = "microsmith.kts",
    compilationConfiguration = MicrosmithScriptDefinition::class
)
abstract class MicrosmithScript(
    protected val microsmithBuilder: MicrosmithBuilder = MicrosmithBuilder()
) {
    val model: MicrosmithModel get() = microsmithBuilder.model
}

object MicrosmithScriptDefinition : ScriptCompilationConfiguration(
    {
        jvm {
            dependenciesFromClassloader(
                "kotlin-stdlib",
                "kotlin-reflect"
            )
        }
        defaultImports(
            "me.liam.microsmith.dsl.core.*",
            "me.liam.microsmith.dsl.schemas.core.*",
            "me.liam.microsmith.dsl.schemas.protobuf.*",
            "me.liam.microsmith.dsl.schemas.protobuf.field.*",
            "me.liam.microsmith.dsl.schemas.protobuf.types.*",
            "me.liam.microsmith.dsl.schemas.protobuf.reserved.*"
        )
        implicitReceivers(MicrosmithBuilder::class)
    }
)
