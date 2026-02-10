package me.liam.microsmith.runtime.scripting.context

import me.liam.microsmith.dsl.core.MicrosmithModel
import java.nio.file.Path

class MicrosmithScriptContext(
    val outDir: Path,
    val vars: Map<String, String>,
    val flags: Set<String>,
    private val emitHandler: (MicrosmithModel) -> Unit
) {
    private var emitted: Boolean = false

    fun emit(model: MicrosmithModel) {
        emitHandler(model)
        emitted = true
    }

    fun generate(model: MicrosmithModel) = emit(model)

    fun hasFlag(name: String): Boolean = flags.contains(name)

    fun varOrNull(name: String): String? = vars[name]

    fun requireVar(name: String): String =
        vars[name] ?: error("Missing required --var '$name'.")

    internal fun emittedAny(): Boolean = emitted
}

