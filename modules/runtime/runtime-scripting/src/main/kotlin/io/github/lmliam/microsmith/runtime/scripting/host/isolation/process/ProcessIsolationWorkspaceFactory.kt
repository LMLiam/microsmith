package io.github.lmliam.microsmith.runtime.scripting.host.isolation.process

import dev.zacsweers.metro.Inject
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.PathWalkOption
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.walk

@Inject
internal class ProcessIsolationWorkspaceFactory {
    fun create(cacheDirectory: Path): ProcessIsolationWorkspace {
        cacheDirectory.createDirectories()
        val workingDirectory = createTempDirectory(cacheDirectory, "process-isolation-")
        return ProcessIsolationWorkspace(
            workingDirectory = workingDirectory,
            requestFile = workingDirectory.resolve("request.properties"),
            resultFile = workingDirectory.resolve("result.properties"),
        )
    }

    @OptIn(ExperimentalPathApi::class)
    fun delete(workspace: ProcessIsolationWorkspace) {
        if (!workspace.workingDirectory.exists()) {
            return
        }

        workspace.workingDirectory.walk(PathWalkOption.INCLUDE_DIRECTORIES).sortedDescending().forEach { entry ->
            entry.deleteIfExists()
        }
    }
}
