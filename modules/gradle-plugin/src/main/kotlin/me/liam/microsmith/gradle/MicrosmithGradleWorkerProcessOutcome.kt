package me.liam.microsmith.gradle

internal data class MicrosmithGradleWorkerProcessOutcome(
    val exitCode: Int,
    val processOutput: String,
)
