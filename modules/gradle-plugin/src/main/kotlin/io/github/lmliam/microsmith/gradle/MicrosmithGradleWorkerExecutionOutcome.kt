package io.github.lmliam.microsmith.gradle

internal data class MicrosmithGradleWorkerExecutionOutcome(
    val exitCode: Int,
    val processOutput: String,
    val parsedResult: MicrosmithGradleWorkerResult?,
)
