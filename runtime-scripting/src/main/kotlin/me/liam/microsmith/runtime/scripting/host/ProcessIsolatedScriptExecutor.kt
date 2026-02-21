package me.liam.microsmith.runtime.scripting.host

import me.liam.microsmith.runtime.scripting.model.ScriptFailureType
import me.liam.microsmith.runtime.scripting.model.ScriptRunFailure
import me.liam.microsmith.runtime.scripting.model.ScriptRunRequest
import me.liam.microsmith.runtime.scripting.model.ScriptRunResult
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

internal class ProcessIsolatedScriptExecutor(private val cacheDirectory: Path) {
    fun execute(request: ScriptRunRequest, scriptPath: Path, outputPath: Path): ScriptRunResult {
        Files.createDirectories(cacheDirectory)
        val workingDirectory = Files.createTempDirectory(cacheDirectory, "process-isolation-")
        val requestFile = workingDirectory.resolve("request.properties")
        val resultFile = workingDirectory.resolve("result.properties")

        return try {
            writeRequest(requestFile, request, scriptPath, outputPath)
            val outcome = executeProcess(requestFile, resultFile)
            mapOutcomeToResult(outcome)
        } catch (error: IOException) {
            failureFromException(error)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            failureFromException(error)
        } catch (error: IllegalArgumentException) {
            failureFromException(error)
        } catch (error: IllegalStateException) {
            failureFromException(error)
        } catch (error: SecurityException) {
            failureFromException(error)
        } finally {
            runCatching { deleteRecursively(workingDirectory) }
        }
    }

    private fun writeRequest(requestFile: Path, request: ScriptRunRequest, scriptPath: Path, outputPath: Path) {
        ProcessIsolationProtocol.writeRequest(
            path = requestFile,
            request =
            ProcessIsolationRequest(
                request = request,
                scriptPath = scriptPath,
                outputPath = outputPath,
                cacheDirectory = cacheDirectory,
            ),
        )
    }

    private fun executeProcess(requestFile: Path, resultFile: Path): ProcessOutcome {
        val command = buildCommand(requestFile, resultFile)
        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val processOutput = process.inputStream.bufferedReader().use { reader -> reader.readText().trim() }
        val exitCode = process.waitFor()
        val parsedResult = runCatching { ProcessIsolationProtocol.readResult(resultFile) }.getOrNull()
        return ProcessOutcome(exitCode = exitCode, processOutput = processOutput, parsedResult = parsedResult)
    }

    private fun mapOutcomeToResult(outcome: ProcessOutcome): ScriptRunResult {
        if (outcome.exitCode == 0 && outcome.parsedResult != null) {
            return outcome.parsedResult
        }
        if (outcome.parsedResult is ScriptRunFailure) {
            return appendProcessOutput(outcome.parsedResult, outcome.processOutput)
        }
        return unknownFailure(outcome.exitCode, outcome.processOutput)
    }

    private fun appendProcessOutput(failure: ScriptRunFailure, processOutput: String): ScriptRunFailure {
        if (processOutput.isEmpty()) {
            return failure
        }
        return failure.copy(diagnostics = failure.diagnostics + "Process stderr/stdout: $processOutput")
    }

    private fun unknownFailure(exitCode: Int, processOutput: String): ScriptRunFailure {
        if (processOutput.isNotEmpty()) {
            return ScriptRunFailure(
                diagnostics = listOf(
                    "Process isolation execution failed with exit code $exitCode.",
                    "Process stderr/stdout: $processOutput",
                ),
                type = ScriptFailureType.HOST,
            )
        }
        return ScriptRunFailure(
            diagnostics = listOf("Process isolation execution failed with exit code $exitCode."),
            type = ScriptFailureType.HOST,
        )
    }

    private fun failureFromException(error: Exception): ScriptRunFailure {
        val message = error.message ?: error::class.simpleName ?: "unknown process isolation error"
        return ScriptRunFailure(
            diagnostics = listOf("Process isolation execution failed: $message"),
            type = ScriptFailureType.HOST,
        )
    }

    private fun buildCommand(requestFile: Path, resultFile: Path): List<String> {
        val javaCommand = resolveJavaCommand()
        val classpath = System.getProperty("java.class.path")
        require(!classpath.isNullOrBlank()) {
            "Process isolation requires a non-empty java.class.path system property."
        }
        val mainClass = ProcessIsolatedScriptMain::class.java.name
        return listOf(
            javaCommand.toString(),
            "-cp",
            classpath,
            mainClass,
            requestFile.toString(),
            resultFile.toString(),
        )
    }

    private fun resolveJavaCommand(): Path {
        val javaHome = Path.of(System.getProperty("java.home"))
        val executableName = if (isWindows()) "java.exe" else "java"
        val executable = javaHome.resolve("bin").resolve(executableName)
        require(Files.exists(executable) && Files.isRegularFile(executable)) {
            "Could not locate Java executable at '$executable' for process isolation mode."
        }
        return executable
    }

    private fun isWindows(): Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("windows")

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { entry ->
                Files.deleteIfExists(entry)
            }
        }
    }

    private data class ProcessOutcome(val exitCode: Int, val processOutput: String, val parsedResult: ScriptRunResult?)
}
