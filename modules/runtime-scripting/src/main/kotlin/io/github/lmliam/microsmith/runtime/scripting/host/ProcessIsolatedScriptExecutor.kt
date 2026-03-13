package io.github.lmliam.microsmith.runtime.scripting.host

import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunRequest
import io.github.lmliam.microsmith.runtime.scripting.model.ScriptRunResult
import java.io.IOException
import java.nio.file.Path

internal class ProcessIsolatedScriptExecutor(
    private val cacheDirectory: Path,
    private val workspaceFactory: ProcessIsolationWorkspaceFactory = ProcessIsolationWorkspaceFactory(),
    private val requestCodec: ProcessIsolationRequestCodec = ProcessIsolationRequestCodec(),
    private val workerLauncher: ProcessIsolationWorkerLauncher = JvmProcessIsolationWorkerLauncher(),
    private val failureFactory: ProcessIsolationFailureFactory = ProcessIsolationFailureFactory(),
) {
    fun execute(request: ScriptRunRequest, scriptPath: Path, outputPath: Path): ScriptRunResult {
        var workspace: ProcessIsolationWorkspace? = null

        return try {
            val createdWorkspace = workspaceFactory.create(cacheDirectory)
            workspace = createdWorkspace
            requestCodec.write(
                path = createdWorkspace.requestFile,
                request =
                ProcessIsolationRequest(
                    request = request,
                    scriptPath = scriptPath,
                    outputPath = outputPath,
                    cacheDirectory = cacheDirectory,
                ),
            )
            failureFactory.fromOutcome(
                workerLauncher.execute(createdWorkspace.requestFile, createdWorkspace.resultFile),
            )
        } catch (error: IOException) {
            failureFactory.fromException(error)
        } catch (error: InterruptedException) {
            Thread.currentThread().interrupt()
            failureFactory.fromException(error)
        } catch (error: IllegalArgumentException) {
            failureFactory.fromException(error)
        } catch (error: IllegalStateException) {
            failureFactory.fromException(error)
        } catch (error: SecurityException) {
            failureFactory.fromException(error)
        } finally {
            workspace?.let { existingWorkspace ->
                runCatching { workspaceFactory.delete(existingWorkspace) }
            }
        }
    }
}
