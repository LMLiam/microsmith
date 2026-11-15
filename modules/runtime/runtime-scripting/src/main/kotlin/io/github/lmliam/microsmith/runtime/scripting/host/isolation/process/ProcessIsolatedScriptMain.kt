package io.github.lmliam.microsmith.runtime.scripting.host.isolation.process

import io.github.lmliam.microsmith.gen.system.SystemConsole
import io.github.lmliam.microsmith.runtime.scripting.createSystemMicrosmithScriptHost
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptFailureType
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptIsolationMode
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunFailure
import java.nio.file.FileSystems
import kotlin.system.exitProcess

internal object ProcessIsolatedScriptMain {
    @JvmStatic
    fun main(args: Array<String>) {
        val console = SystemConsole()
        val fileSystem = FileSystems.getDefault()
        val protocol =
            ProcessIsolationProtocol(
                requestCodec = ProcessIsolationRequestCodec(fileSystem),
                resultCodec = ProcessIsolationResultCodec(fileSystem),
            )

        if (args.size != EXPECTED_ARGS) {
            console.err("Expected 2 arguments: <request.properties> <result.properties>.")
            exitProcess(2)
        }

        val requestFile = fileSystem.getPath(args[0])
        val resultFile = fileSystem.getPath(args[1])

        val result = runCatching {
            val request = protocol.readRequest(requestFile)
            createSystemMicrosmithScriptHost(request.cacheDirectory)
                .run(
                    request.request.copy(
                        script = request.scriptPath,
                        outputDir = request.outputPath,
                        isolationMode = ScriptIsolationMode.CLASSLOADER,
                    ),
                )
        }
            .getOrElse { error ->
                val message = error.message ?: error::class.simpleName ?: "unknown process worker error"
                ScriptRunFailure.withMessages(
                    messages = listOf("Process-isolated worker failure: $message"),
                    type = ScriptFailureType.HOST,
                )
            }

        runCatching {
            protocol.writeResult(resultFile, result)
        }
            .onFailure { error ->
                val message = error.message ?: error::class.simpleName ?: "unknown result-write error"
                console.err("Failed to write process isolation result: $message")
                exitProcess(2)
            }

        exitProcess(0)
    }
}

private const val EXPECTED_ARGS = 2
