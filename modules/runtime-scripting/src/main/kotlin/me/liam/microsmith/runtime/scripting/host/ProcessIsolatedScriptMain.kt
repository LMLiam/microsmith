package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptIsolationMode
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import java.nio.file.Path
import kotlin.system.exitProcess

internal object ProcessIsolatedScriptMain {
    @JvmStatic
    fun main(args: Array<String>) {
        if (args.size != EXPECTED_ARGS) {
            System.err.println("Expected 2 arguments: <request.properties> <result.properties>.")
            exitProcess(2)
        }

        val requestFile = Path.of(args[0])
        val resultFile = Path.of(args[1])

        val result =
            runCatching {
                val request = ProcessIsolationProtocol.readRequest(requestFile)
                ScriptRunExecutor(request.cacheDirectory).execute(
                    request = request.request.copy(isolationMode = ScriptIsolationMode.CLASSLOADER),
                    scriptPath = request.scriptPath,
                    outputPath = request.outputPath,
                )
            }.getOrElse { error ->
                val message = error.message ?: error::class.simpleName ?: "unknown process worker error"
                ScriptRunFailure(
                    diagnostics = listOf("Process-isolated worker failure: $message"),
                    type = ScriptFailureType.HOST,
                )
            }

        runCatching {
            ProcessIsolationProtocol.writeResult(resultFile, result)
        }.onFailure { error ->
            val message = error.message ?: error::class.simpleName ?: "unknown result-write error"
            System.err.println("Failed to write process isolation result: $message")
            exitProcess(2)
        }

        exitProcess(0)
    }
}

private const val EXPECTED_ARGS = 2
