package me.liam.microsmith.runtime.scripting.definition

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    fileExtension = "microsmith.kts",
    compilationConfiguration = MicrosmithScriptCompilationConfiguration::class,
)
@Suppress("UnnecessaryAbstractClass") // Kotlin script template must be a class type.
abstract class MicrosmithScript
