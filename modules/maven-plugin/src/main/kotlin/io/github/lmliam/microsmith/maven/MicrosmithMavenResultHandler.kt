package io.github.lmliam.microsmith.maven

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugin.logging.Log
import java.nio.file.Path
import java.util.Locale

internal class MicrosmithMavenResultHandler {
    fun handle(log: Log, outputDirectory: Path, result: ScriptRunResult) {
        when (result) {
            is ScriptRunSuccess -> handleSuccess(log, outputDirectory, result)
            is ScriptRunFailure -> throw buildFailure(result)
        }
    }

    private fun handleSuccess(log: Log, outputDirectory: Path, result: ScriptRunSuccess) {
        result.warnings.forEach(log::warn)
        val generatedOutputRoot = outputDirectory.toAbsolutePath().normalize().resolve("proto")
        log.info(
            "Generated Microsmith outputs into '$generatedOutputRoot'. " +
                "(compile-cache=${if (result.cacheHit) "hit" else "miss"}, elapsed=${result.elapsedMillis}ms)",
        )
    }

    private fun buildFailure(result: ScriptRunFailure): Exception {
        val message = buildFailureMessage(result)
        return when (result.type) {
            ScriptFailureType.VALIDATION,
            ScriptFailureType.COMPILATION,
            ScriptFailureType.EVALUATION,
            -> MojoFailureException(message)

            ScriptFailureType.HOST -> MojoExecutionException(message)
        }
    }

    private fun buildFailureMessage(result: ScriptRunFailure): String = buildString {
        appendLine("Microsmith generation failed (${result.type.name.lowercase(Locale.ROOT)}).")
        result.diagnostics.forEach(::appendLine)
    }.trimEnd()
}
