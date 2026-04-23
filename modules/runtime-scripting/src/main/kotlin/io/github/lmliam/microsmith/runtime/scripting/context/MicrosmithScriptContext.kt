package io.github.lmliam.microsmith.runtime.scripting.context

import io.github.lmliam.microsmith.dsl.core.MicrosmithModel
import java.nio.file.Path

class MicrosmithScriptContext(
    val outDir: Path,
    val vars: Map<String, String>,
    val flags: Set<String>,
    private val emitHandler: (MicrosmithModel) -> List<Path>,
) {
    private var emitted: Boolean = false
    private var generatedRoots: List<Path> = emptyList()

    fun emit(model: MicrosmithModel) {
        generatedRoots =
            (generatedRoots + emitHandler(model))
                .map { path -> path.toAbsolutePath().normalize() }
                .distinct()
                .sorted()
        emitted = true
    }

    fun generate(model: MicrosmithModel) = emit(model)

    fun hasFlag(name: String): Boolean = flags.contains(name)

    fun varOrNull(name: String): String? = vars[name]

    fun requireVar(name: String): String = vars[name] ?: error("Missing required --var '$name'.")

    internal fun emittedAny(): Boolean = emitted

    internal fun generatedRoots(): List<Path> = generatedRoots
}
