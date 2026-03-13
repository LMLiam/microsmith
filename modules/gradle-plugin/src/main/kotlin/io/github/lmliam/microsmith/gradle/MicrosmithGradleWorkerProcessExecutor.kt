package io.github.lmliam.microsmith.gradle

internal fun interface MicrosmithGradleWorkerProcessExecutor {
    fun execute(command: List<String>): MicrosmithGradleWorkerProcessOutcome
}
