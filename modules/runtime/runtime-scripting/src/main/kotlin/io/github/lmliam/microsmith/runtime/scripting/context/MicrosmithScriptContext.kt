package io.github.lmliam.microsmith.runtime.scripting.context

import io.github.lmliam.microsmith.dsl.MicrosmithModel
import java.nio.file.Path

public class MicrosmithScriptContext
internal constructor(
    public val outDir: Path,
    public val vars: Map<String, String>,
    public val flags: Set<String>,
    private val emitHandler: (MicrosmithModel) -> List<Path>,
) {
    private var emitted: Boolean = false
    private var generatedRoots: List<Path> = emptyList()

    public fun emit(model: MicrosmithModel) {
        generatedRoots =
            (generatedRoots + emitHandler(model)).map { path -> path.toAbsolutePath().normalize() }.distinct().sorted()
        emitted = true
    }

    public fun generate(model: MicrosmithModel): Unit = emit(model)

    public fun hasFlag(name: String): Boolean = flags.contains(name)

    public fun varOrNull(name: String): String? = vars[name]

    public fun requireVar(name: String): String = vars[name] ?: error("Missing required --var '$name'.")

    internal fun emittedAny(): Boolean = emitted

    internal fun generatedRoots(): List<Path> = generatedRoots
}
