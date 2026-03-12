package me.liam.microsmith.runtime.scripting.host

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

internal class ProcessIsolationWorkspaceFactory {
    fun create(cacheDirectory: Path): ProcessIsolationWorkspace {
        Files.createDirectories(cacheDirectory)
        val workingDirectory = Files.createTempDirectory(cacheDirectory, "process-isolation-")
        return ProcessIsolationWorkspace(
            workingDirectory = workingDirectory,
            requestFile = workingDirectory.resolve("request.properties"),
            resultFile = workingDirectory.resolve("result.properties"),
        )
    }

    fun delete(workspace: ProcessIsolationWorkspace) {
        if (!Files.exists(workspace.workingDirectory)) {
            return
        }

        Files.walk(workspace.workingDirectory).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { entry ->
                Files.deleteIfExists(entry)
            }
        }
    }
}
