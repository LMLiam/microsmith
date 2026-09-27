package io.github.lmliam.microsmith.gradle.worker
internal fun interface MicrosmithGradleWorkerProcessExecutor {
    fun execute(command: List<String>): MicrosmithGradleWorkerProcessOutcome
}
