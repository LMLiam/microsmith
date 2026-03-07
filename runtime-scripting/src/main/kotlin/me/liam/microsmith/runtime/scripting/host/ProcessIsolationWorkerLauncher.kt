package me.liam.microsmith.runtime.scripting.host

import java.nio.file.Path

internal interface ProcessIsolationWorkerLauncher {
    fun execute(requestFile: Path, resultFile: Path): ProcessIsolationExecutionOutcome
}
