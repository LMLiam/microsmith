package io.github.lmliam.microsmith.gradle.worker
internal data class MicrosmithGradleWorkerExecutionOutcome(
    val exitCode: Int,
    val processOutput: String,
    val parsedResult: MicrosmithGradleWorkerResult?,
)
