package io.github.lmliam.microsmith.runtime.scripting.host.isolation.process

import java.nio.file.Path

internal interface ProcessIsolationWorkerLauncher {
    fun execute(requestFile: Path, resultFile: Path): ProcessIsolationExecutionOutcome
}
