@file:Suppress("unused")

package me.liam.microsmith.cli.scripting

import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    fileExtension = "microsmith.kts",
    compilationConfiguration = MicrosmithScriptCompilationConfiguration::class
)
abstract class MicrosmithScript
