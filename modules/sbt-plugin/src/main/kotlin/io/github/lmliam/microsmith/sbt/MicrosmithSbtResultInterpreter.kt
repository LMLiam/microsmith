package io.github.lmliam.microsmith.sbt

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Path
import java.util.Locale

class MicrosmithSbtResultInterpreter {
    fun interpret(outputDirectory: Path, result: ScriptRunResult): MicrosmithSbtExecutionOutcome = when (result) {
        is ScriptRunSuccess -> MicrosmithSbtExecutionOutcome(
            outputDirectory = outputDirectory,
            warnings = result.warnings,
            cacheHit = result.cacheHit,
            elapsedMillis = result.elapsedMillis,
        )

        is ScriptRunFailure -> throw buildFailure(result)
    }

    private fun buildFailure(result: ScriptRunFailure): RuntimeException {
        val message = buildFailureMessage(result)
        return when (result.type) {
            ScriptFailureType.VALIDATION,
            ScriptFailureType.COMPILATION,
            ScriptFailureType.EVALUATION,
            -> MicrosmithSbtScriptFailureException(message)

            ScriptFailureType.HOST -> MicrosmithSbtHostFailureException(message)
        }
    }

    private fun buildFailureMessage(result: ScriptRunFailure): String = buildString {
        appendLine("Microsmith generation failed (${result.type.name.lowercase(Locale.ROOT)}).")
        result.diagnostics.forEach(::appendLine)
    }.trimEnd()
}
