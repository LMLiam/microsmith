package me.liam.microsmith.runtime.scripting.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    fileExtension = "microsmith.kts",
    compilationConfiguration = MicrosmithScriptCompilationConfiguration::class
)
abstract class MicrosmithScript

