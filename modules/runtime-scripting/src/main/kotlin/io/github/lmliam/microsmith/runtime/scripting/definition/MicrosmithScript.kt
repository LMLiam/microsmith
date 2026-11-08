package io.github.lmliam.microsmith.runtime.scripting.definition
import kotlin.script.experimental.annotations.KotlinScript

@KotlinScript(
    fileExtension = "microsmith.kts",
    compilationConfiguration = MicrosmithScriptCompilationConfiguration::class,
)
@Suppress("AbstractClassCanBeInterface") // Kotlin script templates must be classes.
abstract class MicrosmithScript
