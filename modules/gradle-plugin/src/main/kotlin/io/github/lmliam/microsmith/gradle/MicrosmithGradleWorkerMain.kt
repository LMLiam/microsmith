package io.github.lmliam.microsmith.gradle

import io.github.lmliam.microsmith.runtime.scripting.host.MicrosmithScriptHost
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunSuccess
import java.nio.file.Path
import kotlin.system.exitProcess

internal object MicrosmithGradleWorkerMain {
    private val requestCodec = MicrosmithGradleWorkerRequestCodec()
    private val resultCodec = MicrosmithGradleWorkerResultCodec()

    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != EXPECTED_ARGS) {
            System.err.println("Expected 2 arguments: <request.properties> <result.properties>.")
            exitProcess(2)
        }

        val requestFile = Path.of(args[0])
        val resultFile = Path.of(args[1])
        val result = runWorker(requestFile)
        runCatching { resultCodec.write(resultFile, result) }
            .onFailure { error ->
                val message = error.message ?: error::class.simpleName ?: "unknown result-write error"
                System.err.println("Failed to write Microsmith Gradle worker result: $message")
                exitProcess(2)
            }
        exitProcess(0)
    }

    private fun runWorker(requestFile: Path): MicrosmithGradleWorkerResult = runCatching {
        val request = requestCodec.read(requestFile)
        val host = MicrosmithScriptHost(cacheDirectory = request.cacheDirectory)
        when (
            val result =
                host.run(
                    ScriptRunRequest(
                        script = request.scriptPath,
                        outputDir = request.outputPath,
                        variables = request.variables,
                        flags = request.flags,
                        pluginClasspath = request.pluginClasspath,
                    ),
                )
        ) {
            is ScriptRunSuccess ->
                MicrosmithGradleWorkerSuccess(
                    warnings = result.warnings,
                    cacheHit = result.cacheHit,
                    elapsedMillis = result.elapsedMillis,
                    generatedRoots = result.generatedRoots,
                )

            is ScriptRunFailure ->
                MicrosmithGradleWorkerFailure(
                    diagnostics = result.diagnostics,
                    type = result.type.name,
                )
        }
    }.getOrElse { error ->
        val message = error.message ?: error::class.simpleName ?: "unknown worker error"
        MicrosmithGradleWorkerFailure(
            diagnostics = listOf("Microsmith Gradle worker failure: $message"),
            type = "HOST",
        )
    }
}

private const val EXPECTED_ARGS = 2
